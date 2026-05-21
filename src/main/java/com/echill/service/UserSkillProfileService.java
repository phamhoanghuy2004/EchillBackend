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

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSkillProfileService {

    private final UserSkillProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public SkillInsightResponse getSkillInsightsByGroup(TagGroup tagGroup) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 🟢 Gọi đúng hàm đặc nhiệm, kéo thẳng Tag Cha từ MySQL lên
        List<UserSkillProfile> parentProfiles = profileRepository.findParentProfilesByUserIdAndTagGroup(userId, tagGroup);

        if (parentProfiles.isEmpty()) {
            throw new AppException(StudentErrorEnum.SKILL_PROFILE_NOT_FOUND);
        }

        // Không cần Stream.filter() nữa! Biến thành SkillDetail luôn.
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

        // 2. Tính điểm trung bình tổng quan
        double overallScore = skillDetails.stream()
                .mapToDouble(SkillInsightResponse.SkillDetail::getScore)
                .average()
                .orElse(0.0);

        // 3. Tìm 2 kỹ năng yếu nhất
        List<String> weakPoints = skillDetails.stream()
                .sorted(Comparator.comparing(SkillInsightResponse.SkillDetail::getScore))
                .limit(2)
                .map(SkillInsightResponse.SkillDetail::getTagName)
                .toList();

        // 4. Sinh lời nhận xét động
        String remark = generateAnalyticalRemark(overallScore, weakPoints);

        return SkillInsightResponse.builder()
                .skills(skillDetails)
                .overallScore(Math.round(overallScore * 10.0) / 10.0)
                .weakPoints(weakPoints)
                .motivationalRemark(remark)
                .build();
    }

    /**
     * 💥 NÂNG CẤP: Lời nhận xét tinh gọn, dựa trực tiếp vào điểm Level hệ 100
     */
    private String generateAnalyticalRemark(double average, List<String> weakPoints) {
        StringBuilder remark = new StringBuilder();

        if (!weakPoints.isEmpty()) {
            String weakSkillsText = String.join(" và ", weakPoints);

            if (average >= 80.0) {
                remark.append(String.format("Nền tảng chung của bạn rất vững chắc. Tuy nhiên, để đạt đến độ hoàn hảo và ẵm trọn điểm tối đa, hãy chú ý mài giũa thêm một chút ở kỹ năng [%s] nhé.", weakSkillsText));
            } else if (average >= 50.0) {
                remark.append(String.format("Phong độ của bạn khá ổn định. Dù vậy, [%s] đang là điểm nghẽn cản bước bạn vươn lên mốc Master. Hãy ưu tiên luyện tập phần này.", weakSkillsText));
            } else {
                remark.append(String.format("Đừng nản lòng! Mọi chuyên gia đều từng là người mới bắt đầu. Hãy tập trung xây dựng lại căn bản, đặc biệt là ở phần [%s].", weakSkillsText));
            }
        } else {
            // Case backup: Lỡ user full điểm không có điểm yếu
            remark.append("Nền tảng của bạn cực kỳ xuất sắc! Không phát hiện lỗ hổng lớn nào. Cứ tiếp tục duy trì phong độ Master này nhé!");
        }

        return remark.toString();
    }
}