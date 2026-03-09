package com.echill.entity;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "transaction_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionItem {
    @Id
    @Tsid
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    Transaction transaction;

    @Column(name = "coins_price")
    Long coinsPrice;

    @Column(name = "amount_price", precision = 12, scale = 0)
    BigDecimal amountPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL"))
    Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE SET NULL"))
    Test test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_package_id", foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (coin_package_id) REFERENCES coin_packages(id) ON DELETE SET NULL"))
    CoinPackage coinPackage;
}
