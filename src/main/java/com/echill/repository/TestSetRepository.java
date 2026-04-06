package com.echill.repository;

import com.echill.entity.TestSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestSetRepository extends JpaRepository<TestSet, Long> {
    Optional<TestSet> findByLessonId(Long lessonId);
}
