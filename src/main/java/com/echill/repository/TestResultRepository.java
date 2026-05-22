package com.echill.repository;

import com.echill.dto.response.TestResultHistoryDto;
import com.echill.entity.TestResult;
import com.echill.entity.enums.TestType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Query("SELECT DISTINCT t.id " +
            "FROM TestResult tr " +
            "JOIN tr.test t " +
            "WHERE tr.student.id = :userId AND t.testSet.id = :testSetId")
    Set<Long> findTakenTestIdsByStudentAndTestSet(@Param("userId") Long userId, @Param("testSetId") Long testSetId);

    @Query("""
        SELECT new com.echill.dto.response.TestResultHistoryDto(
            tr.id, t.id, t.title, tr.totalScore, tr.timeTakenSeconds, tr.isPassed, tr.createdAt
        )
        FROM TestResult tr
        JOIN tr.test t
        WHERE tr.student.id = :studentId
          AND (:testId IS NULL OR t.id = :testId)
          AND (:testTitle IS NULL OR t.title LIKE :testTitle)
          AND (:startDate IS NULL OR tr.createdAt >= :startDate)
          AND (:endDate IS NULL OR tr.createdAt <= :endDate)
    """)
    Page<TestResultHistoryDto> getMyHistoryOptimized(
            @Param("studentId") Long studentId,
            @Param("testId") Long testId,
            @Param("testTitle") String testTitle,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(tr) > 0 " +
            "FROM TestResult tr " +
            "JOIN tr.test t " +
            "WHERE tr.student.id = :studentId AND t.type = 'PLACEMENT_TEST'")
    boolean existsByStudentIdAndPlacementTest(@Param("studentId") Long studentId);

    @Query("SELECT new com.echill.dto.response.TestResultHistoryDto(" +
            "tr.id, t.id, t.title, tr.totalScore, tr.timeTakenSeconds, tr.isPassed, tr.createdAt) " +
            "FROM TestResult tr " +
            "JOIN tr.test t " +
            "WHERE tr.student.id = :studentId " +
            "AND tr.totalQuestions = 200 " +
            "AND t.type = :testType " +
            "ORDER BY tr.createdAt DESC")
    List<TestResultHistoryDto> findTopRecentFullTests(
            @Param("studentId") Long studentId,
            @Param("testType") TestType testType,
            Pageable pageable
    );
}
