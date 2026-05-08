package com.echill.repository;

import com.echill.entity.DailyStudyTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;


@Repository
public interface DailyStudyTimeRepository extends JpaRepository<DailyStudyTime, Long> {
    // 1. Lấy tổng thời lượng học trong tuần
    @Query("SELECT COALESCE(SUM(d.totalSeconds), 0) " +
            "FROM DailyStudyTime d " +
            "WHERE d.user.id = :userId AND d.studyDate BETWEEN :startDate AND :endDate")
    Long sumUserStudySecondsInDateRange(@Param("userId") Long userId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    // 2. ATOMIC UPDATE: Cộng trực tiếp dưới DB để chống Race Condition
    // Trả về số dòng được update (Nếu = 0 nghĩa là user chưa học hôm nay, cần INSERT mới)
    @Modifying
    @Query("UPDATE DailyStudyTime d SET d.totalSeconds = d.totalSeconds + :seconds WHERE d.user.id = :userId AND d.studyDate = :studyDate")
    int addSecondsAtomic(@Param("userId") Long userId,
                         @Param("studyDate") LocalDate studyDate,
                         @Param("seconds") Long seconds);

    // Lấy toàn bộ record của user trong 1 tháng
    @Query("SELECT d FROM DailyStudyTime d WHERE d.user.id = :userId AND d.studyDate >= :startDate AND d.studyDate <= :endDate")
    List<DailyStudyTime> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
