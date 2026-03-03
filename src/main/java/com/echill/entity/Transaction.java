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
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    // Mã giao dịch từ đối tác (VD: VNPay gửi về). Không cho phép sửa sau khi tạo.
    @Column(name = "transaction_code", unique = true, length = 100, updatable = false)
    String transactionCode;

    // Số xu thay đổi (VD: -100 xu, +500 xu).
    @Column(name = "coins_changed", nullable = false, updatable = false)
    Long coinsChanged;

    // Số tiền VNĐ thanh toán thực tế (nếu có nạp tiền).
    @Column(precision = 12, scale = 0, updatable = false)
    BigDecimal amount;

    // Số dư xu của User NGAY SAU KHI giao dịch thành công (Rất quan trọng để đối soát)
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

    // --- CÁC LIÊN KẾT: BẢO VỆ CHẶT CHẼ BẰNG SET NULL ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    User user; // Giao dịch bắt buộc phải có User. (Không nên cascade delete User nếu có dính giao dịch)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL"))
    Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE SET NULL"))
    Test test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_package_id", foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (coin_package_id) REFERENCES coin_packages(id) ON DELETE SET NULL"))
    CoinPackage coinPackage;

    // ==========================================
    // HELPER METHODS
    // ==========================================

    public void markAsSuccess(Long currentBalance) {
        this.status = TransactionStatus.SUCCESS;
        this.balanceAfter = currentBalance; // Chốt số dư tại thời điểm thành công
    }

    public void markAsFailed() {
        this.status = TransactionStatus.FAILED;
    }
}
