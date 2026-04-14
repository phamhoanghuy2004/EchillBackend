package com.echill.repository;

import com.echill.dto.response.TeacherStudentResponse;
import com.echill.entity.Course;
import com.echill.entity.Enrollment;
import com.echill.entity.User;
import com.echill.repository.projection.MyCourseProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudentAndCourse(User student, Course course);

    @Query("SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId")
    Set<Long> findCourseIdsByStudentId(@Param("studentId") Long studentId);

    @Query("""
        SELECT e.id AS enrollmentId,
               c.id AS courseId,
               c.name AS courseName,
               c.imageUrl AS courseImage,
               c.totalLessonsCount AS totalLessons,
               t.fullName AS teacherName,
               t.avatarUrl AS teacherAvatar,
               e.lastAccessedAt AS lastAccessedAt,
               COALESCE(SUM(CASE WHEN lp.isCompleted = true 
                                  AND lp.versionCompleted IS NOT NULL 
                                  AND lp.versionCompleted = l.version 
                             THEN 1 ELSE 0 END), 0) AS completedLessons
        FROM Enrollment e
        JOIN e.course c
        JOIN c.teacher t
        LEFT JOIN LessonProgress lp ON lp.enrollment = e
        LEFT JOIN lp.lesson l
        WHERE e.student.id = :studentId
        GROUP BY e.id, c.id, c.name, c.imageUrl, c.totalLessonsCount, t.fullName, t.avatarUrl, e.lastAccessedAt
    """)
    Page<MyCourseProjection> findMyCoursesWithProgress(@Param("studentId") Long studentId, Pageable pageable);

    // ✅ DTO Projection chuẩn senior: DB xử lý tất cả, không load full entity, hỗ trợ phân trang
    @Query("""
            SELECT new com.echill.dto.response.TeacherStudentResponse(
                e.id,
                s.fullName,
                s.email,
                s.avatarUrl,
                c.name,
                c.id,
                e.completedLessonsCount,
                c.totalLessonsCount,
                e.createdAt
            )
            FROM Enrollment e
            JOIN e.student s
            JOIN e.course c
            WHERE c.teacher.id = :teacherId
            """)
    Page<TeacherStudentResponse> findStudentStatistics(
            @Param("teacherId") Long teacherId,
            Pageable pageable
    );
}
