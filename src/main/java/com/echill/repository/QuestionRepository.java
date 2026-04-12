package com.echill.repository;

import com.echill.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("SELECT q FROM Question q " +
            "JOIN FETCH q.section s " +
            "JOIN FETCH s.test t " +
            "JOIN FETCH t.testSet ts " +
            "JOIN FETCH ts.user u " +
            "LEFT JOIN FETCH q.answers a " +
            "WHERE q.id = :questionId")
    Optional<Question> findByIdWithFullRelations(@Param("questionId") Long questionId);

    @Query("SELECT q FROM Question q WHERE q.section.test.id = :testId")
    List<Question> findByTestId(@Param("testId") Long testId);

    @Query("SELECT COUNT(q) FROM Question q JOIN q.section s WHERE s.test.id = :testId")
    Integer countTotalQuestionsByTestId(@Param("testId") Long testId);
}
