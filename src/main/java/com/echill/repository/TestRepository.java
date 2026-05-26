package com.echill.repository;

import com.echill.entity.Test;
import com.echill.repository.projection.TestQuestionCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByTestSetId(Long testSetId);

    @Query("SELECT t.id FROM Test t WHERE t.testSet.id = :testSetId")
    List<Long> findTestIdsByTestSetId(@Param("testSetId") Long testSetId);

    Boolean existsByIdAndTestSetId(Long testId, Long testSetId);

    @Query("SELECT t.id as testId, COUNT(DISTINCT q.id) as totalQuestions " +
            "FROM Test t " +
            "LEFT JOIN t.sections s " +
            "LEFT JOIN s.questions q " +
            "WHERE t.testSet.id = :testSetId " +
            "GROUP BY t.id")
    List<TestQuestionCountProjection> countQuestionsByTestSetId(@Param("testSetId") Long testSetId);

    @Query("SELECT t FROM Test t JOIN FETCH t.testSet WHERE t.id = :testId")
    Optional<Test> findByIdWithTestSet(@Param("testId") Long testId);

    @Query("SELECT DISTINCT t FROM Test t " +
            "JOIN FETCH t.testSet ts " +
            "JOIN FETCH t.sections s " +
            "LEFT JOIN FETCH s.questions q " +
            "LEFT JOIN FETCH q.tag " +
            "LEFT JOIN FETCH q.questionGroup " +
            "WHERE t.id = :testId")
    Optional<Test> findByIdWithSectionsAndQuestions(@Param("testId") Long testId);
}
