package com.echill.dto.response;

import com.echill.entity.enums.Level;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String name;
    String description;
    BigDecimal price;
    BigDecimal originalPrice;
    String imageUrl;
    Level level;
    com.echill.entity.enums.Status status;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long categoryId;
    String categoryName;
    String teacherName;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long teacherId;
    String teacherAvatarUrl;
    LocalDateTime createdAt;
    List<TagResponse> tags;
    java.util.List<LessonResponse> lessons;
    Double averageRating;
    Long studentCount;
    Long reviewCount;
}
