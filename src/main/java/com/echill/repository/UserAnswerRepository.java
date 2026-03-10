package com.echill.repository;

import com.echill.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
    @Query("SELECT ua FROM UserAnswer ua " +
            "JOIN FETCH ua.question q " +
            "LEFT JOIN FETCH q.tag " +
            "LEFT JOIN FETCH ua.selectedAnswer a " +
            "WHERE ua.testResult.id = :testResultId")
    List<UserAnswer> findAllDetailsByTestResultId(@Param("testResultId") Long testResultId);
}
