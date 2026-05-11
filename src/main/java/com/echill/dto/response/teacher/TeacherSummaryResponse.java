package com.echill.dto.response.teacher;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherSummaryResponse {
    long totalCourses;
    BigDecimal totalRevenue;
    long totalStudents;
    double averageRating;
}
