package com.echill.repository;

import com.echill.entity.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.id = :id") // 💥 BẮT BUỘC PHẢI CÓ DÒNG NÀY
    Optional<Voucher> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT v FROM Voucher v WHERE v.creator.id = :creatorId")
    Page<Voucher> findVouchersByCreatorId(@Param("creatorId") Long creatorId, Pageable pageable);

    @Query("SELECT v FROM Voucher v WHERE v.isActive = true " +
            "AND v.startDate <= :now AND v.endDate >= :now " +
            "AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    List<Voucher> findValidPublicVouchers(@Param("now") Instant now);

    @Query("SELECT v FROM Voucher v " +
            "WHERE v.isActive = true " +
            "AND v.startDate <= :now AND v.endDate >= :now " +
            "AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit) " +
            "AND v.minCourseCount >= 2 " + // 💥 Điều kiện cốt lõi cho Combo
            "ORDER BY v.minCourseCount ASC, v.createdAt DESC")
    List<Voucher> findActiveComboVouchers(@Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.code = :code")
    Optional<Voucher> findByCodeWithLock(@Param("code") String code);
}
