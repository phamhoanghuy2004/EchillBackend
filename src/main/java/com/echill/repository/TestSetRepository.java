package com.echill.repository;

import com.echill.entity.TestSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestSetRepository extends JpaRepository<TestSet, Long> {
    Optional<TestSet> findByLessonId(Long lessonId);

    @Query("SELECT t.lesson.id FROM TestSet t WHERE t.id = :testSetId")
    Optional<Long> findLessonIdByTestSetId(@Param("testSetId") Long testSetId);

    java.util.List<TestSet> findAllByUserId(Long userId);
}
