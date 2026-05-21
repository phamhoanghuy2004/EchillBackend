package com.echill.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkillInsightResponse {

    List<SkillDetail> skills; // Dùng để vẽ Radar Chart

    Double overallScore; // Điểm trung bình (Hệ 100)

    List<String> weakPoints; // Các điểm yếu cần khắc phục

    String motivationalRemark; // Lời nhận xét

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SkillDetail {
        Long tagId;
        String tagName;
        Double score;         // Điểm hệ 100 để vẽ UI
        String masteryLevel;  // Truyền thêm để UI đổi màu (Tùy chọn)
    }
}