package com.echill.repository;

import com.echill.entity.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user", "items", "items.coinPackage", "items.course", "voucher"})
    @Query("SELECT t FROM Transaction t WHERE t.transactionCode = :txnCode")
    Optional<Transaction> findByTransactionCodeForUpdate(@Param("txnCode") String txnCode);

    @EntityGraph(attributePaths = {"items", "items.course", "voucher"})
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.status = 'PENDING'")
    List<Transaction> findAllPendingTransactionsByUser(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"items", "items.coinPackage"})
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.status = 'PENDING'")
    List<Transaction> findAllCoinPackagePendingTransactionsByUser(@Param("userId") Long userId);

    @Query("""
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = 'SYSTEM_BONUS'
          AND t.status = 'SUCCESS'
          AND t.createdAt >= :startDate
          AND t.createdAt < :endDate
    ) THEN true ELSE false END
""")
    boolean existsBonusTransactionInWeek(
            @Param("userId") Long userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}
