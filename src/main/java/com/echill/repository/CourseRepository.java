package com.echill.repository;

import com.echill.entity.Course;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    @Query("SELECT c FROM Course c WHERE c.teacher.username = :username AND c.status != 'DELETED'")
    List<Course> findByTeacherUsername(@Param("username") String username);

    @Query("""
        SELECT c FROM Course c
        JOIN FETCH c.teacher
        WHERE c.id = :id
    """)
    Optional<Course> findByIdWithTeacher(@Param("id") Long id);

    // 💥 GỘP 4 BẢNG VÀO 1 CÂU SQL (Course + Category + Teacher + Lessons)
    @Query("SELECT DISTINCT c FROM Course c " +
            "JOIN FETCH c.category " +
            "JOIN FETCH c.teacher " +
            "LEFT JOIN FETCH c.lessons " +
            "WHERE c.teacher.id = :teacherId")
    List<Course> findAllByTeacherIdWithDetails(@Param("teacherId") Long teacherId);

    // 💥 Tương tự cho hàm lấy chi tiết 1 khóa học
    @Query("SELECT c FROM Course c " +
            "JOIN FETCH c.category " +
            "JOIN FETCH c.teacher " +
            "LEFT JOIN FETCH c.lessons " +
            "WHERE c.id = :courseId")
    Optional<Course> findByIdWithDetails(@Param("courseId") Long courseId);
}
