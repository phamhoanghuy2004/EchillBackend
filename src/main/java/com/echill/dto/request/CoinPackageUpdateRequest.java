package com.echill.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoinPackageUpdateRequest {
    @NotBlank(message = "Tên gói xu không được để trống")
    String name;

    @NotNull(message = "Giá bán không được để trống")
    @Min(value = 0, message = "Giá bán không được nhỏ hơn 0")
    BigDecimal price;

    @NotNull(message = "Số xu gốc không được để trống")
    @Min(value = 1, message = "Số xu tối thiểu phải là 1")
    Long coinAmount;

    @Min(value = 0, message = "Xu tặng thêm không được nhỏ hơn 0")
    Long bonusCoin = 0L;

    @Min(value = 0, message = "Giá gốc không được nhỏ hơn 0")
    BigDecimal originalPrice;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    Boolean isActive;
}
