package com.echill.event.listener;

import com.echill.entity.Tag;
import com.echill.entity.User;
import com.echill.entity.UserSkillProfile;
import com.echill.entity.enums.TestType;
import com.echill.event.TestEvaluatedEvent;
import com.echill.repository.TagRepository;
import com.echill.repository.UserRepository;
import com.echill.repository.UserSkillProfileRepository;
import com.echill.service.SkillTrackingService;
import com.echill.service.StudentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SkillEventListener {

    UserSkillProfileRepository profileRepository;
    TagRepository tagRepository;
    UserRepository userRepository;
    SkillTrackingService skillTrackingService;
    StudentService studentService;

    @Async("skillProfileTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSkillEvolution(TestEvaluatedEvent event) {
        if (event.testType() == TestType.PLACEMENT_TEST) return;

        Long userId = event.userId();
        Map<Long, Integer> effectiveScores = event.tagLevelScores();

        if (effectiveScores == null || effectiveScores.isEmpty()) return;

        log.info("🔄 [BACKGROUND] Xử lý Profiling User {} - {} tags", userId, effectiveScores.size());

        Set<Long> tagIds = effectiveScores.keySet();
        User userProxy = userRepository.getReferenceById(userId);

        List<UserSkillProfile> existingProfiles = profileRepository.findAllByUserIdAndTagIdIn(userId, tagIds);
        Map<Long, UserSkillProfile> profileMap = existingProfiles.stream()
                .collect(Collectors.toMap(p -> p.getTag().getId(), p -> p));

        Set<Long> missingTagIds = tagIds.stream()
                .filter(id -> !profileMap.containsKey(id))
                .collect(Collectors.toSet());

        Map<Long, Tag> tagMap = new HashMap<>();
        if (!missingTagIds.isEmpty()) {
            List<Tag> realTags = tagRepository.findAllById(missingTagIds);
            tagMap = realTags.stream().collect(Collectors.toMap(Tag::getId, t -> t));
        }

        List<UserSkillProfile> profilesToSave = new ArrayList<>();
        Set<Long> affectedParentTagIds = new HashSet<>();

        for (Map.Entry<Long, Integer> entry : effectiveScores.entrySet()) {
            Long tagId = entry.getKey();
            Integer effectiveLevel = entry.getValue();

            UserSkillProfile profile = profileMap.get(tagId);
            if (profile == null) {
                Tag realTag = tagMap.get(tagId);
                profile = UserSkillProfile.builder()
                        .user(userProxy)
                        .tag(realTag)
                        .currentLevel(0)
                        .isInferred(false)
                        .build();
            }

            int oldLevel = profile.getCurrentLevel();
            int newLevel = Boolean.TRUE.equals(profile.getIsInferred())
                    ? effectiveLevel
                    : (int) Math.round((oldLevel * 0.7) + (effectiveLevel * 0.3));

            log.info("tagId: {}, oldLevel: {}", tagId, oldLevel);
            log.info("newLevel: {}, newLevel: {}", tagId, newLevel);


            profile.updateSkill(newLevel, false);
            profilesToSave.add(profile);

            if (profile.getTag() != null && profile.getTag().getParent() != null) {
                affectedParentTagIds.add(profile.getTag().getParent().getId());
            }
        }

        profileRepository.saveAll(profilesToSave);

        if (!affectedParentTagIds.isEmpty()) {
            log.info("Kích hoạt Skill Roll-up lan truyền lên {} Tag Cha", affectedParentTagIds.size());
            skillTrackingService.updateParentSkillLevelsBatch(userId, affectedParentTagIds);
            studentService.updateOverallStudentLevel(userId, false);
        }
    }
}