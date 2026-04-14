package com.echill.repository;

import com.echill.dto.response.TeacherStudentResponse;
import com.echill.entity.Course;
import com.echill.entity.Enrollment;
import com.echill.entity.User;
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
