package com.echill.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopStudentResponse {
    private Long id;
    private String name;
    private String avatar;
    private TopScoreDto scores;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopScoreDto {
        private Double reading;
        private Double listening;
        private Double total;
    }
}
