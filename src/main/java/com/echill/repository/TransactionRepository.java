package com.echill.repository;

import com.echill.entity.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.transactionCode = :code")
    Optional<Transaction> findByTransactionCodeForUpdate(@Param("code") String code);


    @EntityGraph(attributePaths = {"items", "items.course", "voucher"})
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.status = 'PENDING'")
    List<Transaction> findAllPendingTransactionsByUser(@Param("userId") Long userId);
}
