package com.echill.dto.response.admin;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminSummaryResponse {
    BigDecimal totalRevenue;
    long totalCourses;
    long totalStudents;
    long totalTeachers;
}
