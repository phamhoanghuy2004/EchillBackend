package com.echill.repository;

import com.echill.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    // Spring Data JPA sẽ tự dịch ra:
    // SELECT COUNT(*) > 0 FROM lesson_progresses lp JOIN enrollments e ON lp.enrollment_id = e.id WHERE lp.lesson_id = ? AND e.student_id = ?
    boolean existsByLessonIdAndEnrollmentStudentId(Long lessonId, Long studentId);
    Optional<LessonProgress> findByLessonIdAndEnrollmentStudentId(Long lessonId, Long userId);

    @Modifying
    @Query(value = "UPDATE lesson_progresses lp " +
            "INNER JOIN enrollments e ON lp.enrollment_id = e.id " +
            "SET lp.last_watched_second = GREATEST(lp.last_watched_second, :currentSecond) " +
            "WHERE e.student_id = :userId AND lp.lesson_id = :lessonId", nativeQuery = true)
    void updateAtomicProgress(Long lessonId, Long userId, Integer currentSecond);

    @Query("SELECT lp FROM LessonProgress lp " +
            "JOIN FETCH lp.lesson l " +
            "LEFT JOIN FETCH l.testSet ts " +
            "WHERE l.id = :lessonId AND lp.enrollment.student.id = :userId")
    Optional<LessonProgress> findProgressWithLessonAndTestSet(
            @Param("lessonId") Long lessonId,
            @Param("userId") Long userId);
}
