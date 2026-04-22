package com.echill.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutRequest {
    @NotNull(message = "Danh sách khóa học không được để null")
    @NotEmpty(message = "Vui lòng chọn ít nhất 1 khóa học để thanh toán")
    List<Long> courseIds;
}
