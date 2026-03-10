package com.echill.entity;

import com.echill.entity.enums.DiscountType;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
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

    public boolean isValid() {
        Instant now = Instant.now();
        return isActive
                && now.isAfter(startDate)
                && now.isBefore(endDate)
                && (usageLimit == null || usedCount < usageLimit);
    }
}