package com.echill.entity;

import com.echill.entity.enums.TransactionStatus;
import com.echill.entity.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Transaction extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @Column(name = "transaction_code", unique = true, length = 100, updatable = false)
    String transactionCode;

    @Column(name = "vnp_transaction_no", unique = true, length = 255, updatable = false)
    String vnpTransactionNo; // Lưu mã đối soát của VNPAY/Ngân hàng

    // đây là tông số su mà user đã thanh toán trong giao dịch (bao gồmm giảm giá đồ luôn)
    @Column(name = "total_coins_changed", nullable = false, updatable = false)
    Long totalCoinsChanged;

    // đây là tổng số tiền mà user đã thanh toán trong giao dịch (bao gồm giảm giá đồ luôn)
    @Column(name = "total_amount", precision = 12, scale = 0, updatable = false)
    BigDecimal totalAmount;

    // đây là số xu còn lại ngay sau khi giao dịch này thành công (để dành đối chiếu)
    @Column(name = "balance_after")
    Long balanceAfter;

    @Column(length = 255)
    String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    User user;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<TransactionItem> items = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE SET NULL"))
    Voucher voucher;

    @Column(name = "discount_amount", precision = 12, scale = 0)
    BigDecimal discountAmount;

    @Column(name = "expired_at")
    Instant expiredAt;


    public void addItem(TransactionItem item) {
        items.add(item);
        item.setTransaction(this);
    }

    public void removeItems() {
        for (TransactionItem item : items) {
            item.setTransaction(null);
        }
        this.items.clear();
    }

    public void markAsSuccess(Long currentBalance) {
        this.status = TransactionStatus.SUCCESS;
        this.balanceAfter = currentBalance;
    }

    public void markAsFailed() {
        this.status = TransactionStatus.FAILED;
    }
}
