package com.echill.event;

import com.echill.entity.Tag;
import com.echill.entity.User;
import com.echill.entity.UserSkillProfile;
import com.echill.entity.enums.TestType;
import com.echill.repository.StudentProfileRepository;
import com.echill.repository.TagRepository;
import com.echill.repository.UserRepository;
import com.echill.repository.UserSkillProfileRepository;
import com.echill.service.UserLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SkillProfileListener {

    private final UserSkillProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final UserLevelService userLevelService;

    @Async("skillProfileTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSkillEvolution(TestEvaluatedEvent event) {
        log.info("📈 [Async] Bắt đầu tính toán Skill Profile cho User ID: {}", event.userId());

        if (event.tagScores() == null || event.tagScores().isEmpty()) {
            return;
        }

        try {
            User userRef = userRepository.getReferenceById(event.userId());

            updateSkillProfiles(event, userRef);

            if (TestType.PLACEMENT_TEST.equals(event.testType())) {
                userLevelService.processLevelEvolution(event.userId(), event.tagScores(), userRef);
            }

            log.info("✅ [Async] Hoàn tất xử lý dữ liệu năng lực cho User ID: {}", event.userId());

        } catch (Exception e) {
            log.error("❌ [Async] Lỗi nghiêm trọng khi cập nhật Skill Profile cho User ID: {}", event.userId(), e);
        }

    }

    private void updateSkillProfiles(TestEvaluatedEvent event, User userRef) {
        List<UserSkillProfile> existingProfiles = profileRepository.findByUserIdAndTagIdIn(
                event.userId(),
                event.tagScores().keySet()
        );

        Map<Long, UserSkillProfile> profileMap = existingProfiles.stream()
                .collect(Collectors.toMap(p -> p.getTag().getId(), p -> p));

        List<UserSkillProfile> profilesToSave = new ArrayList<>();

        event.tagScores().forEach((tagId, currentScore) -> {
            UserSkillProfile profile = profileMap.get(tagId);

            if (profile == null) {
                Tag tagRef = tagRepository.getReferenceById(tagId);
                UserSkillProfile newProfile = UserSkillProfile.builder()
                        .user(userRef)
                        .tag(tagRef)
                        .proficiencyPercentage(currentScore)
                        .build();
                profilesToSave.add(newProfile);

            } else {
                double weight = 0.3;
                double oldScore = profile.getProficiencyPercentage();
                double newProficiency = (oldScore * (1 - weight)) + (currentScore * weight);

                profile.updateProficiency(newProficiency);
                profilesToSave.add(profile);
            }
        });

        if (!profilesToSave.isEmpty()) {
            profileRepository.saveAll(profilesToSave);
        }
    }

}