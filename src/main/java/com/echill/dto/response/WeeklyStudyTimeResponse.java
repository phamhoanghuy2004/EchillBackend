package com.echill.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeeklyStudyTimeResponse {
    // Tổng số giây user đã học trong tuần này
    Long currentSeconds;

    // Mục tiêu cần đạt được (VD: 10 giờ = 36000 giây)
    Long targetSeconds;

    Boolean isClaimed;
}
