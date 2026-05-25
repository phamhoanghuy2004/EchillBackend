package com.echill.service;

import com.echill.dto.response.SkillInsightResponse;
import com.echill.entity.StudentProfile;
import com.echill.entity.UserSkillProfile;
import com.echill.entity.enums.TagGroup;
import com.echill.exception.AppException;
import com.echill.exception.StudentErrorEnum;
import com.echill.repository.StudentProfileRepository;
import com.echill.repository.UserSkillProfileRepository;
import com.echill.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSkillProfileService {

    private final UserSkillProfileRepository profileRepository;
    private final StudentProfileRepository studentProfileRepository;

    @Transactional(readOnly = true)
    public SkillInsightResponse getSkillInsightsByGroup(TagGroup tagGroup) {
        Long userId = SecurityUtils.getCurrentUserId();

        List<UserSkillProfile> parentProfiles = profileRepository.findParentProfilesByUserIdAndTagGroup(userId, tagGroup);

        if (parentProfiles.isEmpty()) {
            throw new AppException(StudentErrorEnum.SKILL_PROFILE_NOT_FOUND);
        }

        List<SkillInsightResponse.SkillDetail> skillDetails = parentProfiles.stream()
                .map(p -> {
                    double percentageScore = 0.0;
                    if (p.getTag().getMaxLevel() != null && p.getTag().getMaxLevel() > 0) {
                        percentageScore = ((double) p.getCurrentLevel() / p.getTag().getMaxLevel()) * 100.0;
                    }
                    return SkillInsightResponse.SkillDetail.builder()
                            .tagId(p.getTag().getId())
                            .tagName(p.getTag().getName())
                            .score(Math.round(percentageScore * 100.0) / 100.0)
                            .masteryLevel(p.getMasteryLevel().name())
                            .build();
                })
                .toList();

        double overallScore = skillDetails.stream()
                .mapToDouble(SkillInsightResponse.SkillDetail::getScore)
                .average()
                .orElse(0.0);

        List<String> weakSkills = skillDetails.stream()
                .filter(s -> "BEGINNER".equals(s.getMasteryLevel()))
                .map(SkillInsightResponse.SkillDetail::getTagName)
                .toList();

        List<String> averageSkills = skillDetails.stream()
                .filter(s -> "INTERMEDIATE".equals(s.getMasteryLevel()))
                .map(SkillInsightResponse.SkillDetail::getTagName)
                .toList();

        List<String> strongSkills = skillDetails.stream()
                .filter(s -> "ADVANCED".equals(s.getMasteryLevel()))
                .map(SkillInsightResponse.SkillDetail::getTagName)
                .toList();

        String remark = generateAnalyticalRemark(weakSkills, averageSkills, strongSkills);

        return SkillInsightResponse.builder()
                .skills(skillDetails)
                .overallScore(Math.round(overallScore * 10.0) / 10.0)
                .weakPoints(weakSkills)
                .motivationalRemark(remark)
                .build();
    }

    /**
     * 💥 NÂNG CẤP: Lời nhận xét chia khối rõ ràng, cá nhân hóa theo từng nhóm kỹ năng
     */
    private String generateAnalyticalRemark(List<String> weakSkills, List<String> averageSkills, List<String> strongSkills) {
        StringBuilder remark = new StringBuilder();

        if (!weakSkills.isEmpty() && averageSkills.isEmpty() && strongSkills.isEmpty()) {
            String weakText = String.join(", ", weakSkills);
            return String.format("Đừng nản lòng! Mọi chuyên gia đều từng là người mới bắt đầu. Hãy tập trung xây dựng lại căn bản từ các phần [%s] nhé.", weakText);
        }

        if (weakSkills.isEmpty() && averageSkills.isEmpty() && !strongSkills.isEmpty()) {
            return "Nền tảng của bạn cực kỳ xuất sắc và không phát hiện lỗ hổng nào! Hãy tiếp tục duy trì phong độ đỉnh cao này ở mọi kỹ năng nhé.";
        }

        if (!strongSkills.isEmpty()) {
            String strongText = String.join(", ", strongSkills);
            remark.append(String.format("Bạn đang nắm rất vững nền tảng ở kỹ năng [%s]. ", strongText));
        }

        if (!averageSkills.isEmpty()) {
            String averageText = String.join(", ", averageSkills);
            remark.append(String.format("Hãy tiếp tục duy trì và luyện tập thêm để nâng tầm phần [%s]. ", averageText));
        }

        if (!weakSkills.isEmpty()) {
            String weakText = String.join(", ", weakSkills);
            remark.append(String.format("Đặc biệt, bạn cần chú ý khắc phục lỗ hổng kiến thức ở phần [%s] để vươn lên cấp độ cao hơn.", weakText));
        }

        return remark.toString().trim();
    }

    @Transactional(readOnly = true)
    public List<SkillInsightResponse.SkillDetail> getChildSkillInsights(Long parentTagId) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<UserSkillProfile> childProfiles = profileRepository.findAllByUserIdAndTagParentId(userId, parentTagId);

        return childProfiles.stream()
                .map(p -> {
                    double percentageScore = 0.0;
                    if (p.getTag().getMaxLevel() != null && p.getTag().getMaxLevel() > 0) {
                        percentageScore = ((double) p.getCurrentLevel() / p.getTag().getMaxLevel()) * 100.0;
                    }
                    return SkillInsightResponse.SkillDetail.builder()
                            .tagId(p.getTag().getId())
                            .tagName(p.getTag().getName())
                            .score(Math.round(percentageScore * 100.0) / 100.0)
                            .masteryLevel(p.getMasteryLevel().name())
                            .build();
                })
                .toList();
    }

    // ===== ADAPTIVE LEARNING (Bước 1-3) =====

    /**
     * Tìm Tag lỗ hổng ưu tiên nhất của user.
     * Bước 1: Xác định Target Level từ StudentProfile.
     * Bước 2: Quét các Tag Con có currentLevel < targetLevel.
     * Bước 3: DB đã sort theo minLevel ASC, currentLevel ASC → lấy phần tử đầu.
     *
     * @return Optional<UserSkillProfile> - Tag con có lỗ hổng ưu tiên nhất
     */
    @Transactional(readOnly = true)
    public Optional<UserSkillProfile> findTopKnowledgeGap(Long userId) {
        // Bước 1: Lấy Level từ StudentProfile → xác định targetLevel
        StudentProfile sp = studentProfileRepository.findByUserId(userId)
                .orElse(null);
        if (sp == null) return Optional.empty();

        int targetLevel = switch (sp.getLevel()) {
            case BEGINNER -> 3;
            case INTERMEDIATE -> 4;
            case ADVANCED -> 5;
            default -> 3; // UNDETERMINED → dùng mức thấp nhất
        };

        // Bước 2-3: 1 query duy nhất, DB sort sẵn, lấy phần tử đầu
        List<UserSkillProfile> gaps = profileRepository.findKnowledgeGaps(userId, targetLevel);

        return gaps.stream().findFirst();
    }

    /**
     * Tính Target Level cho user dựa trên StudentProfile.
     */
    public int getTargetLevel(Long userId) {
        StudentProfile sp = studentProfileRepository.findByUserId(userId)
                .orElse(null);
        if (sp == null) return 3;

        return switch (sp.getLevel()) {
            case BEGINNER -> 3;
            case INTERMEDIATE -> 4;
            case ADVANCED -> 5;
            default -> 3;
        };
    }
}