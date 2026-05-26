package com.echill.repository;

import com.echill.dto.response.learner.TestSetRecommendationResponse;
import com.echill.entity.TestSet;
import com.echill.entity.enums.TestType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestSetRepository extends JpaRepository<TestSet, Long>, JpaSpecificationExecutor<TestSet> {
    Optional<TestSet> findByLessonId(Long lessonId);

    @Query("SELECT t.lesson.id FROM TestSet t WHERE t.id = :testSetId")
    Optional<Long> findLessonIdByTestSetId(@Param("testSetId") Long testSetId);

    @Query("SELECT new com.echill.dto.response.learner.TestSetRecommendationResponse(" +
            "t.id, t.title, t.description, t.year, SIZE(t.tests)) " +
            "FROM TestSet t " +
            "WHERE t.year = :year " +
            "AND t.type IN :types " +
            "ORDER BY t.createdAt DESC")
    List<TestSetRecommendationResponse> findRecommendedTestSets(@Param("year") Integer year,
                                                                @Param("types") List<TestType> types,
                                                                Pageable pageable);

    java.util.List<TestSet> findAllByUserId(Long userId);

    @EntityGraph(attributePaths = {"tests"})
    @Query("SELECT ts FROM TestSet ts WHERE ts.id = :testSetId")
    Optional<TestSet> findByIdWithTests(@Param("testSetId") Long testSetId);

    @Query("SELECT c.id FROM TestSet ts JOIN ts.lesson l JOIN l.course c WHERE ts.id = :testSetId")
    Optional<Long> findCourseIdByTestSetId(@Param("testSetId") Long testSetId);

    Optional<TestSet> findByUserIdAndTitle(Long userId, String title);

}
