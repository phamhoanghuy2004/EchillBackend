package com.echill.repository;

import com.echill.entity.TestSession;
import com.echill.entity.enums.TestSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
    Optional<TestSession> findFirstByStudentIdAndTestSetIdAndStatusOrderByCreatedAtDesc(
            Long studentId,
            Long testSetId,
            TestSessionStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TestSession ts SET ts.status = :newStatus, ts.activeLock = NULL " +
            "WHERE ts.id = :sessionId AND ts.status = :oldStatus")
    int updateStatusConditionally(
            @Param("sessionId") Long sessionId,
            @Param("newStatus") TestSessionStatus newStatus,
            @Param("oldStatus") TestSessionStatus oldStatus
    );

}
