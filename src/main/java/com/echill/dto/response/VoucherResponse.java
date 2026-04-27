package com.echill.dto.response;

import com.echill.entity.enums.DiscountType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String code;
    DiscountType discountType;
    BigDecimal discountValue;
    BigDecimal maxDiscountAmount;
    BigDecimal minOrderValue;
    Instant startDate;
    Instant endDate;
    Integer usageLimit;
    Integer usedCount;
    Integer minCourseCount;
    Boolean isAutoApplied;
    Boolean isActive;
}
