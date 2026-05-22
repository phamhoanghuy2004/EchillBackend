package com.echill.service;

import com.echill.dto.response.SkillInsightResponse;
import com.echill.entity.UserSkillProfile;
import com.echill.entity.enums.TagGroup;
import com.echill.exception.AppException;
import com.echill.exception.StudentErrorEnum;
import com.echill.repository.UserSkillProfileRepository;
import com.echill.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSkillProfileService {

    private final UserSkillProfileRepository profileRepository;

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
}