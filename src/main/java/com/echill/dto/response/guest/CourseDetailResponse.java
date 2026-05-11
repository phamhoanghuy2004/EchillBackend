package com.echill.dto.response.guest;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseDetailResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String name;
    String description;
    BigDecimal price;
    BigDecimal originalPrice;
    Integer discountPercent;
    String imageUrl;
    String level;

    // 💥 Gộp thông tin liên quan
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long categoryId;
    String categoryName;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long teacherId;
    String teacherName;
    String teacherAvatarUrl;

    List<LessonPublicResponse> lessons;
    Double averageRating;
    Long studentCount;
    Long reviewCount;
}