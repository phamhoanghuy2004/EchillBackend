package com.echill.repository;

import com.echill.entity.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    @Query("SELECT SUM(ti.amountPrice) FROM TransactionItem ti " +
           "JOIN ti.transaction t " +
           "WHERE ti.course.teacher.id = :teacherId AND t.status = 'SUCCESS'")
    BigDecimal getTotalRevenueByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT SUM(ti.amountPrice) FROM TransactionItem ti " +
           "JOIN ti.transaction t " +
           "WHERE ti.course.teacher.id = :teacherId AND ti.course.id = :courseId AND t.status = 'SUCCESS'")
    BigDecimal getTotalRevenueByTeacherIdAndCourseId(@Param("teacherId") Long teacherId, @Param("courseId") Long courseId);

    @Query(value = "SELECT " +
           "  CASE " +
           "    WHEN :period = 'DAY' THEN DATE_FORMAT(ti.created_at, '%Y-%m-%d') " +
           "    WHEN :period = 'WEEK' THEN DATE_FORMAT(ti.created_at, '%Y-%u') " +
           "    WHEN :period = 'MONTH' THEN DATE_FORMAT(ti.created_at, '%Y-%m') " +
           "    WHEN :period = 'YEAR' THEN DATE_FORMAT(ti.created_at, '%Y') " +
           "  END as label, " +
           "  SUM(ti.amount_price) as value " +
           "FROM transaction_items ti " +
           "JOIN transactions t ON ti.transaction_id = t.id " +
           "JOIN courses c ON ti.course_id = c.id " +
           "WHERE c.teacher_id = :teacherId " +
           "AND (:courseId IS NULL OR c.id = :courseId) " +
           "AND t.status = 'SUCCESS' " +
           "GROUP BY label " +
           "ORDER BY label ASC", nativeQuery = true)
    List<Object[]> getRevenueByPeriod(@Param("teacherId") Long teacherId, @Param("courseId") Long courseId, @Param("period") String period);
}
