package com.echill.repository;

import com.echill.entity.TestSession;
import com.echill.entity.enums.TestSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
    Optional<TestSession> findFirstByStudentIdAndTestSetIdAndStatusOrderByCreatedAtDesc(
            Long studentId,
            Long testSetId,
            TestSessionStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TestSession s JOIN FETCH s.test WHERE s.id = :sessionId")
    Optional<TestSession> findByIdWithLockAndFetchTest(@Param("sessionId") Long sessionId);
}
