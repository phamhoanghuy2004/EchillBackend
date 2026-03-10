package com.echill.entity;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "coin_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoinPackage extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @Column(nullable = false, length = 100)
    String name;

    @Column(nullable = false, precision = 12, scale = 0)
    BigDecimal price;

    @Column(name = "coin_amount", nullable = false)
    Long coinAmount;

    @Column(name = "bonus_coin", nullable = false)
    @Builder.Default
    Long bonusCoin = 0L; // Mặc định là 0 nếu không có xu tặng kèm

    @Column(name = "original_price", precision = 12, scale = 0)
    BigDecimal originalPrice;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;

    @Transient
    public Integer getDiscountPercent() {
        if (this.originalPrice == null
                || this.originalPrice.compareTo(BigDecimal.ZERO) == 0
                || this.price.compareTo(this.originalPrice) >= 0) {
            return 0;
        }

        BigDecimal discountAmount = this.originalPrice.subtract(this.price);
        BigDecimal percentage = discountAmount.divide(this.originalPrice, 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        return percentage.intValue();
    }
}