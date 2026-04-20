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

    List<SkillDetail> skills;

    Double overallScore;

    List<String> weakPoints;

    List<String> improvedSkills;

    List<String> declinedSkills;

    String motivationalRemark;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SkillDetail {
        Long tagId;
        String tagName;
        Double score;
    }
}