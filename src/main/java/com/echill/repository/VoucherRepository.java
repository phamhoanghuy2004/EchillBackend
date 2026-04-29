package com.echill.repository;

import com.echill.entity.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount + 1 " +
            "WHERE v.code = :code AND v.isActive = true AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    int atomicReserveVoucherSlot(@Param("code") String code);

    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount - 1 " +
            "WHERE v.code = :code AND v.usedCount > 0")
    int atomicReleaseVoucherSlot(@Param("code") String code);

    Optional<Voucher> findVoucherByCode(@Param("code") String code);
}
