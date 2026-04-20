package com.echill.service;

import com.echill.dto.response.SkillInsightResponse;
import com.echill.entity.UserSkillProfile;
import com.echill.entity.enums.TagGroup;
import com.echill.exception.AppException;
import com.echill.exception.StudentErrorEnum;
import com.echill.repository.UserSkillProfileRepository;
import com.echill.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;


@Service
@RequiredArgsConstructor
public class UserSkillProfileService {

    private final UserSkillProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public SkillInsightResponse getSkillInsightsByGroup(TagGroup tagGroup) {
        Long userId = SecurityUtils.getCurrentUserId();

        List<UserSkillProfile> profiles = profileRepository.findByUserIdAndTagGroup(userId, tagGroup);

        if (profiles.isEmpty()) {
            throw new AppException(StudentErrorEnum.SKILL_PROFILE_NOT_FOUND);
        }

        List<SkillInsightResponse.SkillDetail> skillDetails = profiles.stream()
                .map(p -> SkillInsightResponse.SkillDetail.builder()
                        .tagId(p.getTag().getId())
                        .tagName(p.getTag().getName())
                        .score(p.getProficiencyPercentage())
                        .build())
                .toList();

        double overallScore = profiles.stream()
                .mapToDouble(UserSkillProfile::getProficiencyPercentage)
                .average()
                .orElse(0.0);

        List<String> weakPoints = profiles.stream()
                .limit(2)
                .map(p -> p.getTag().getName())
                .toList();

        List<String> improvedSkills = profiles.stream()
                .filter(p -> p.getLatestDelta() > 1.0)
                .sorted(Comparator.comparing(UserSkillProfile::getLatestDelta).reversed())
                .limit(1)
                .map(p -> p.getTag().getName())
                .toList();

        List<String> declinedSkills = profiles.stream()
                .filter(p -> p.getLatestDelta() < -1.0)
                .sorted(Comparator.comparing(UserSkillProfile::getLatestDelta))
                .limit(1)
                .map(p -> p.getTag().getName())
                .toList();

        String remark = generateAnalyticalRemark(overallScore, weakPoints, improvedSkills, declinedSkills);

        return SkillInsightResponse.builder()
                .skills(skillDetails)
                .overallScore(Math.round(overallScore * 100.0) / 100.0)
                .weakPoints(weakPoints)
                .improvedSkills(improvedSkills)
                .declinedSkills(declinedSkills)
                .motivationalRemark(remark)
                .build();
    }

    // 💥 NÂNG CẤP: Dùng String.join cho tất cả để Scale thoải mái
    private String generateAnalyticalRemark(double average, List<String> weakPoints,
                                            List<String> improved, List<String> declined) {
        StringBuilder remark = new StringBuilder();

        // PHẦN 1: Khen ngợi tiến bộ (Nếu có)
        if (!improved.isEmpty()) {
            String improvedText = String.join(", ", improved); // Nối bằng dấu phẩy hoặc " và "
            remark.append(String.format("🌟 Tuyệt vời! So với lần trước, bạn đã có sự tiến bộ rõ rệt ở kỹ năng [%s]. ", improvedText));
        }

        // PHẦN 2: Đánh giá tổng quan & Chỉ ra điểm yếu
        if (!weakPoints.isEmpty()) {
            String weakSkillsText = String.join(" và ", weakPoints);
            if (average >= 80.0) {
                remark.append(String.format("Nền tảng chung của bạn rất vững. Tuy nhiên, để đạt độ hoàn hảo, hãy chú ý mài giũa thêm một chút ở [%s] nhé.", weakSkillsText));
            } else if (average >= 60.0) {
                remark.append(String.format("Phong độ của bạn khá ổn định. Dù vậy, [%s] đang là điểm nghẽn kéo điểm số xuống. Hãy ưu tiên luyện tập phần này.", weakSkillsText));
            } else {
                remark.append(String.format("Đừng nản lòng! Hãy tập trung xây dựng lại căn bản, đặc biệt là ở phần [%s].", weakSkillsText));
            }
        } else {
            // Case backup: Lỡ user full 100 điểm không có điểm yếu
            remark.append("Nền tảng của bạn xuất sắc và không phát hiện lỗ hổng lớn nào. Cứ tiếp tục giữ vững phong độ nhé!");
        }

        // PHẦN 3: Cảnh báo đi lùi (Nếu có)
        if (!declined.isEmpty()) {
            String declinedText = String.join(", ", declined);
            remark.append(String.format(" ⚠️ Lưu ý: Kỹ năng [%s] đang có dấu hiệu giảm sút, bạn nhớ ôn tập lại nhé!", declinedText));
        }

        return remark.toString();
    }
}