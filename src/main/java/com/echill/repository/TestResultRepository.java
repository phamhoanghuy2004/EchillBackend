package com.echill.repository;

import com.echill.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    @Query("SELECT tr FROM TestResult tr " +
            "JOIN FETCH tr.test t " +
            "WHERE tr.student.id = :userId AND t.testSet.id = :testSetId " +
            "ORDER BY tr.createdAt DESC")
    List<TestResult> findHistoryByStudentAndTestSet(@Param("userId") Long userId, @Param("testSetId") Long testSetId);
}
