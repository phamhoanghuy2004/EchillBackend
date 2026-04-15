package com.echill.repository;

import com.echill.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    // Spring Data JPA sẽ tự dịch ra:
    // SELECT COUNT(*) > 0 FROM lesson_progresses lp JOIN enrollments e ON lp.enrollment_id = e.id WHERE lp.lesson_id = ? AND e.student_id = ?
    boolean existsByLessonIdAndEnrollmentStudentId(Long lessonId, Long studentId);
}
