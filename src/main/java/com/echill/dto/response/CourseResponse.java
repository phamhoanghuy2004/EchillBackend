package com.echill.dto.response;

import com.echill.entity.enums.Level;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    String id;
    String name;
    String description;
    BigDecimal price;
    BigDecimal originalPrice;
    String imageUrl;
    Level level;
    Long categoryId;
    String categoryName;
    String teacherName;
    LocalDateTime createdAt;
    java.util.List<LessonResponse> lessons;
}
