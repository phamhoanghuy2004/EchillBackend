package com.echill.dto.request;

import com.echill.entity.enums.Level;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseRequest {

    @NotBlank(message = "Course name is required")
    String name;

    @NotBlank(message = "Course description is required")
    String description;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be positive or zero")
    BigDecimal price;

    @PositiveOrZero(message = "Original price must be positive or zero")
    BigDecimal originalPrice;

    @NotNull(message = "Level is required")
    Level level;

    @NotNull(message = "Category ID is required")
    Long categoryId;
}
