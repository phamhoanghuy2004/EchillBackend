package com.echill.repository;

import com.echill.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    @Query("SELECT c FROM Course c JOIN FETCH c.teacher WHERE c.id = :id")
    Optional<Course> findByIdWithTeacher(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM Course c " +
            "JOIN FETCH c.category " +
            "JOIN FETCH c.teacher " +
            "LEFT JOIN FETCH c.lessons " +
            "WHERE c.teacher.id = :teacherId")
    List<Course> findAllByTeacherIdWithDetails(@Param("teacherId") Long teacherId);

    @Query("SELECT c FROM Course c " +
            "JOIN FETCH c.category " +
            "JOIN FETCH c.teacher " +
            "LEFT JOIN FETCH c.lessons " +
            "WHERE c.id = :courseId")
    Optional<Course> findByIdWithDetails(@Param("courseId") Long courseId);

//    @Query("SELECT DISTINCT c FROM Course c " +
//            "JOIN FETCH c.category " +
//            "JOIN FETCH c.teacher " +
//            "LEFT JOIN FETCH c.lessons l " +       // 💥 Đặt alias 'l' cho lessons
//            "LEFT JOIN FETCH l.documents " +       // 💥 Móc từ 'l' để lôi documents lên
//            "WHERE c.teacher.id = :teacherId")
//    List<Course> findAllByTeacherIdWithDetails(@Param("teacherId") Long teacherId);
//
//    @Query("SELECT c FROM Course c " +
//            "JOIN FETCH c.category " +
//            "JOIN FETCH c.teacher " +
//            "LEFT JOIN FETCH c.lessons l " +       // 💥 Đặt alias 'l' cho lessons
//            "LEFT JOIN FETCH l.documents " +       // 💥 Móc từ 'l' để lôi documents lên
//            "WHERE c.id = :courseId")
//    Optional<Course> findByIdWithDetails(@Param("courseId") Long courseId);

    @Query("SELECT c.imagePublicId FROM Course c WHERE c.imagePublicId IS NOT NULL")
    List<String> findAllImagePublicIds();
}
