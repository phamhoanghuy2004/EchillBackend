package com.echill.repository;

import com.echill.entity.Course;
import com.echill.entity.User;
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

    @Query("SELECT DISTINCT c FROM Course c " +
            "LEFT JOIN FETCH c.lessons l " +
            "WHERE c.id = :id")
    Optional<Course> findByIdWithLessons(@Param("id") Long id);
}
