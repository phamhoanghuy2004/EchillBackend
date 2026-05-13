package com.echill.dto.request.elasticsearch.response;

import com.echill.entity.enums.Level;
import com.echill.entity.enums.Status;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter // Bắt buộc cho MapStruct
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseCardResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    String name;

    BigDecimal price;

    BigDecimal originalPrice;

    Integer discountPercent;

    String imageUrl;

    Level level;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long categoryId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long teacherId;

    String categoryName;

    String teacherName;

    String categoryDescription;

    String teacherAvatarUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    Instant createdAt;

    Status status;
    Double averageRating;
    Long studentCount;
    Long reviewCount;
}
