package com.echill.repository;

import com.echill.entity.Course;
import com.echill.entity.Review;
import com.echill.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByUserAndCourse(User user, Course course);
    List<Review> findByCourseIdOrderByCreatedAtDesc(Long courseId);
    
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.course.id = :courseId")
    Double getAverageRatingByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT r.course.id, AVG(r.rating) FROM Review r WHERE r.course.id IN :courseIds GROUP BY r.course.id")
    List<Object[]> getAverageRatingsByCourseIds(@Param("courseIds") List<Long> courseIds);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.course.id = :courseId")
    Long countByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT r.course.id, COUNT(r) FROM Review r WHERE r.course.id IN :courseIds GROUP BY r.course.id")
    List<Object[]> countReviewsByCourseIds(@Param("courseIds") List<Long> courseIds);

    org.springframework.data.domain.Page<Review> findByCourseIdOrderByCreatedAtDesc(Long courseId, org.springframework.data.domain.Pageable pageable);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.course.teacher.id = :teacherId")
    Double getAverageRatingByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.course.teacher.id = :teacherId AND r.course.id = :courseId")
    Double getAverageRatingByTeacherIdAndCourseId(@Param("teacherId") Long teacherId, @Param("courseId") Long courseId);
}
