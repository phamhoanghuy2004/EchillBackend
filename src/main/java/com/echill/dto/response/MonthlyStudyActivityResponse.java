package com.echill.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MonthlyStudyActivityResponse {
    Map<Integer, Long> dailyData;
}
