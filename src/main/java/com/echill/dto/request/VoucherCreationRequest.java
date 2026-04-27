package com.echill.dto.request;

import com.echill.entity.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherCreationRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    @Size(max = 50, message = "Mã voucher tối đa 50 ký tự")
    String code;

    @NotNull(message = "Loại giảm giá không được để trống")
    DiscountType discountType;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá trị giảm phải lớn hơn 0")
    BigDecimal discountValue;

    BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0.0", message = "Giá trị đơn hàng tối thiểu không được âm")
    BigDecimal minOrderValue;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @FutureOrPresent(message = "Ngày bắt đầu phải ở hiện tại hoặc tương lai")
    Instant startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @Future(message = "Ngày kết thúc phải ở tương lai")
    Instant endDate;

    @Min(value = 1, message = "Giới hạn sử dụng phải lớn hơn 0")
    Integer usageLimit;

    @Min(value = 1, message = "Số lượng khóa học tối thiểu phải từ 1 trở lên")
    Integer minCourseCount;

    Boolean isAutoApplied;

}
