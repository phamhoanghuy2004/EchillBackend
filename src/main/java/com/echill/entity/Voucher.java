package com.echill.entity;

import com.echill.entity.enums.DiscountType;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Voucher extends BaseEntity {

    @Id
    @Tsid
    Long id;

    @Column(unique = true, nullable = false, length = 50)
    String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    DiscountType discountType;


    @Column(name = "discount_value", precision = 12, scale = 0, nullable = false)
    BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 12, scale = 0)
    BigDecimal maxDiscountAmount;

    @Column(name = "min_order_value", precision = 12, scale = 0)
    BigDecimal minOrderValue;

    @Column(name = "start_date", nullable = false)
    Instant startDate;

    @Column(name = "end_date", nullable = false)
    Instant endDate;

    @Column(name = "usage_limit")
    Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    Integer usedCount = 0;

    @Column(name = "min_course_count")
    Integer minCourseCount;

    @Column(name = "is_auto_applied", nullable = false)
    @Builder.Default
    Boolean isAutoApplied = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    User creator;

    public boolean isValid() {
        Instant now = Instant.now();
        return isActive
                && (now.compareTo(startDate) >= 0)
                && (now.compareTo(endDate) <= 0)
                && (usageLimit == null || usedCount < usageLimit);
    }

    public void validateApplicability(BigDecimal totalOrderValue, int courseCount) {
        if (!isValid()) {
            throw new AppException(ErrorEnum.VOUCHER_CONDITION_NOT_MET);
        }
        if (minOrderValue != null && totalOrderValue.compareTo(minOrderValue) < 0) {
            throw new AppException(ErrorEnum.VOUCHER_CONDITION_NOT_MET);
        }
        if (minCourseCount != null && courseCount < minCourseCount) {
            throw new AppException(ErrorEnum.VOUCHER_CONDITION_NOT_MET);
        }
    }

    public BigDecimal calculateDiscount(BigDecimal totalOrderValue) {
        if (discountType == DiscountType.VALUE) {
            return discountValue;
        }

        BigDecimal discount = totalOrderValue.multiply(discountValue)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            return maxDiscountAmount;
        }
        return discount;
    }
}