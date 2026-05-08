package com.echill.repository;

import com.echill.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    @Query("SELECT tr FROM TestResult tr " +
            "JOIN FETCH tr.test t " +
            "WHERE tr.student.id = :userId AND t.testSet.id = :testSetId " +
            "ORDER BY tr.createdAt DESC")
    List<TestResult> findHistoryByStudentAndTestSet(@Param("userId") Long userId, @Param("testSetId") Long testSetId);

    @Query("SELECT COUNT(tr) FROM TestResult tr " +
            "WHERE tr.student.id = :studentId AND tr.test.testSet.id = :testSetId")
    long countByStudentAndTestSet(@Param("studentId") Long studentId,
                                  @Param("testSetId") Long testSetId);

    @Query("SELECT DISTINCT tr.test.id FROM TestResult tr WHERE tr.student.id = :studentId AND tr.test.testSet.id = :testSetId")
    List<Long> findTakenTestIds(@Param("studentId") Long studentId, @Param("testSetId") Long testSetId);

    @Query("SELECT tr FROM TestResult tr " +
            "JOIN FETCH tr.student " +
            "JOIN FETCH tr.test " +
            "WHERE tr.id = :resultId")
    Optional<TestResult> findByIdWithDetails(@Param("resultId") Long resultId);


    Optional<TestResult> findBySessionId(Long sessionId);

    @Query("SELECT COUNT(tr) FROM TestResult tr " +
            "WHERE tr.student.id = :userId " +
            "AND tr.createdAt BETWEEN :startDate AND :endDate")
    Long countTestsTakenInDateRange(@Param("userId") Long userId,
                                    @Param("startDate") Instant startDate,
                                    @Param("endDate") Instant endDate);
}
