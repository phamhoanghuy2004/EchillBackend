package com.echill.service;

import com.echill.entity.Tag;
import com.echill.entity.User;
import com.echill.entity.UserSkillProfile;
import com.echill.repository.TagRepository;
import com.echill.repository.UserRepository;
import com.echill.repository.UserSkillProfileRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SkillTrackingService {

    UserSkillProfileRepository profileRepository;
    TagRepository tagRepository;
    UserRepository userRepository;

    /**
     * THUẬT TOÁN 1: KNOWLEDGE TRACING (Tối ưu Batch Processing)
     */
    @Transactional
    public void processPlacementTestFinish(Long userId, Long parentTagId, int baselineLevel, Map<Long, Integer> testedTagResults) {
        log.info("Bắt đầu Inferred Scoring cho User {} nhánh Tag Cha {}", userId, parentTagId);

        // 🟢 Proxy Optimization: Không gọi SELECT User/Tag từ DB, chỉ mượn ID để gán khóa ngoại
        User userProxy = userRepository.getReferenceById(userId);
        Tag parentTagProxy = tagRepository.getReferenceById(parentTagId);

        // 1. Lấy danh sách Tag con
        List<Tag> allChildTags = tagRepository.findByParentId(parentTagId);

        // 2. Gom tất cả ID (Cha + Con) để query 1 lần duy nhất (Anti N+1 Query)
        List<Long> allTagIds = new ArrayList<>();
        allTagIds.add(parentTagId);
        allChildTags.forEach(t -> allTagIds.add(t.getId()));

        // Lấy tất cả Profile cũ lên và map theo TagId cho dễ tra cứu O(1)
        List<UserSkillProfile> existingProfiles = profileRepository.findAllByUserIdAndTagIdIn(userId, allTagIds);
        Map<Long, UserSkillProfile> profileMap = existingProfiles.stream()
                .collect(Collectors.toMap(p -> p.getTag().getId(), p -> p));

        List<UserSkillProfile> profilesToSave = new ArrayList<>();

        // 3. Xử lý lưu điểm Tag Cha
        UserSkillProfile parentProfile = profileMap.getOrDefault(parentTagId,
                UserSkillProfile.builder().user(userProxy).tag(parentTagProxy).build());
        parentProfile.updateSkill(baselineLevel, false);
        profilesToSave.add(parentProfile);

        // 4. Vòng lặp Auto-fill cho Tag Con (Xử lý trên RAM)
        for (Tag tag : allChildTags) {
            int inferredLevel;
            boolean isInferred;

            if (testedTagResults.containsKey(tag.getId())) {
                inferredLevel = testedTagResults.get(tag.getId());
                isInferred = false;
            } else {
                if (baselineLevel >= tag.getMaxLevel()) {
                    inferredLevel = tag.getMaxLevel();
                } else if (baselineLevel < tag.getMinLevel()) {
                    inferredLevel = 0;
                } else {
                    inferredLevel = baselineLevel;
                }
                isInferred = true;
            }

            UserSkillProfile childProfile = profileMap.getOrDefault(tag.getId(),
                    UserSkillProfile.builder().user(userProxy).tag(tag).build());
            childProfile.updateSkill(inferredLevel, isInferred);
            profilesToSave.add(childProfile);
        }

        // 🟢 BATCH INSERT/UPDATE: DB chỉ thực thi 1 luồng lưu duy nhất cho hàng chục records
        profileRepository.saveAll(profilesToSave);
        log.info("Hoàn tất Inferred Scoring cho User {}. Đã lưu {} records.", userId, profilesToSave.size());
    }

    /**
     * THUẬT TOÁN 2: SKILL ROLL-UP
     */
    @Transactional
    public void updateParentSkillLevelsBatch(Long userId, Set<Long> parentTagIds) {
        if (parentTagIds == null || parentTagIds.isEmpty()) return;

        log.info("Bắt đầu tính toán Batch Roll-up cho User {} với {} nhánh Tag Cha", userId, parentTagIds.size());

        List<UserSkillProfile> allChildProfiles = profileRepository.findAllChildProfilesByParentIds(userId, parentTagIds);

        Map<Long, List<UserSkillProfile>> childrenByParentId = allChildProfiles.stream()
                .collect(Collectors.groupingBy(p -> p.getTag().getParent().getId()));

        Map<Long, UserSkillProfile> existingParentProfiles = profileRepository.findParentProfilesByIds(userId, parentTagIds)
                .stream().collect(Collectors.toMap(p -> p.getTag().getId(), p -> p));

        List<UserSkillProfile> parentProfilesToSave = new ArrayList<>();
        User userProxy = userRepository.getReferenceById(userId); // User ID lấy Proxy là an toàn vì update không đụng vào thuộc tính của User

        for (Long parentId : parentTagIds) {
            List<UserSkillProfile> childProfiles = childrenByParentId.getOrDefault(parentId, new ArrayList<>());
            if (childProfiles.isEmpty()) continue;

            int totalCurrentLevel = 0;
            int totalMaxLevel = 0;

            for (UserSkillProfile child : childProfiles) {
                totalCurrentLevel += child.getCurrentLevel();
                totalMaxLevel += child.getTag().getMaxLevel();
            }

            if (totalMaxLevel == 0) continue;

            double masteryRatio = (double) totalCurrentLevel / totalMaxLevel;
            int newParentLevel = (int) Math.round(masteryRatio * 5);
            newParentLevel = Math.max(1, Math.min(5, newParentLevel));

            UserSkillProfile parentProfile = existingParentProfiles.get(parentId);
            if (parentProfile == null) {
                Tag realParentTag = childProfiles.getFirst().getTag().getParent();

                parentProfile = UserSkillProfile.builder()
                        .user(userProxy)
                        .tag(realParentTag)
                        .build();
            }

            parentProfile.updateSkill(newParentLevel, false);
            parentProfilesToSave.add(parentProfile);
        }

        if (!parentProfilesToSave.isEmpty()) {
            profileRepository.saveAll(parentProfilesToSave);
            log.info("Batch Roll-up thành công cho {} Tag Cha", parentProfilesToSave.size());
        }
    }
}