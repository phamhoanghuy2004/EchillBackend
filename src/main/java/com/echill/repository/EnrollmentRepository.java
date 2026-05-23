package com.echill.repository;

import com.echill.dto.response.TeacherStudentResponse;
import com.echill.entity.Course;
import com.echill.entity.Enrollment;
import com.echill.entity.User;
import com.echill.entity.enums.EnrollmentStatus;
import com.echill.repository.projection.MyCourseProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
import java.util.Set;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    @Query("SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId")
    Set<Long> findCourseIdsByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT e.course.id, e.course.name FROM Enrollment e WHERE e.student.id = :studentId")
    List<Object[]> findCourseBasicInfoByStudentId(@Param("studentId") Long studentId);

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
        AND c.status = 'ACTIVE'
        GROUP BY e.id, c.id, c.name, c.imageUrl, c.totalLessonsCount, t.fullName, t.avatarUrl, e.lastAccessedAt
    """)
    Page<MyCourseProjection> findMyCoursesWithProgress(@Param("studentId") Long studentId, Pageable pageable);


    @Query("""
            SELECT new com.echill.dto.response.TeacherStudentResponse(
                e.id,
                s.id,
                s.fullName,
                s.email,
                s.avatarUrl,
                c.name,
                c.id,
                COALESCE(SUM(CASE WHEN lp.isCompleted = true 
                                  AND lp.versionCompleted IS NOT NULL 
                                  AND lp.versionCompleted = l.version 
                             THEN 1L ELSE 0L END), 0L),
                c.totalLessonsCount,
                e.createdAt
            )
            FROM Enrollment e
            JOIN e.student s
            JOIN e.course c
            LEFT JOIN LessonProgress lp ON lp.enrollment = e
            LEFT JOIN lp.lesson l
            WHERE c.teacher.id = :teacherId
            GROUP BY e.id, s.id, s.fullName, s.email, s.avatarUrl, c.name, c.id, c.totalLessonsCount, e.createdAt
            """)
    List<TeacherStudentResponse> findStudentStatistics(@Param("teacherId") Long teacherId);


    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course WHERE e.student.id = :studentId AND e.course.id = :courseId")
    Optional<Enrollment> findByStudentIdAndCourseId(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId
    );

    @Query("SELECT e.course.id FROM Enrollment e WHERE e.student.id = :userId AND e.course.id IN :courseIds")
    List<Long> findOwnedCourseIds(@Param("userId") Long userId, @Param("courseIds") List<Long> courseIds);

    @EntityGraph(attributePaths = {"course"})
    Optional<Enrollment> findFirstByStudentIdAndEnrollmentStatusOrderByLastAccessedAtDesc(
            Long studentId,
            EnrollmentStatus status
    );

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId")
    Long countByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT e.course.id, COUNT(e) FROM Enrollment e WHERE e.course.id IN :courseIds GROUP BY e.course.id")
    List<Object[]> countEnrollmentsByCourseIds(@Param("courseIds") List<Long> courseIds);

    @Query("SELECT COUNT(DISTINCT e.student.id) FROM Enrollment e WHERE e.course.teacher.id = :teacherId")
    long countStudentsByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT COUNT(DISTINCT e.student.id) FROM Enrollment e WHERE e.course.teacher.id = :teacherId AND e.course.id = :courseId")
    long countStudentsByTeacherIdAndCourseId(@Param("teacherId") Long teacherId, @Param("courseId") Long courseId);

    @Query("SELECT e.course.id, e.course.name, COUNT(e) as enrollmentCount " +
           "FROM Enrollment e " +
           "WHERE e.course.teacher.id = :teacherId " +
           "GROUP BY e.course.id, e.course.name " +
           "ORDER BY enrollmentCount DESC")
    List<Object[]> findTopSellingCoursesByTeacherId(@Param("teacherId") Long teacherId);
}
