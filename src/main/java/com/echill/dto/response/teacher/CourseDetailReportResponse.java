package com.echill.dto.response.teacher;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseDetailReportResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long courseId;
    String courseName;
    String teacherName;
    String teacherAvatar;
    BigDecimal revenue;
    long salesCount;
    double averageRating;
    int rank;
}
