package com.echill.repository;

import com.echill.entity.Course;
import com.echill.entity.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
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

    @Query("SELECT c.imagePublicId FROM Course c WHERE c.imagePublicId IS NOT NULL")
    List<String> findAllImagePublicIds();

    // Các câu lệnh dưới đây dùng cho guest
    @Query("SELECT c FROM Course c " +
            "JOIN FETCH c.category " +
            "JOIN FETCH c.teacher " +
            "LEFT JOIN FETCH c.lessons l " +
            "LEFT JOIN FETCH l.testSet ts " +
            "WHERE c.id = :id AND c.status = 'ACTIVE'")
    Optional<Course> findActiveCourseWithFullDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM Course c " +
            "JOIN FETCH c.category " +
            "JOIN FETCH c.teacher " +
            "WHERE c.status = 'ACTIVE' " +
            "ORDER BY c.createdAt DESC")
    List<Course> findAllActiveCoursesSortedByNewest();

    Optional<Course> findByIdAndStatus(Long id, Status status);

    @Query("SELECT t.id FROM Course c JOIN c.tags t WHERE c.id = :courseId")
    List<Long> findTagIdsByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT c.id, t.id FROM Course c JOIN c.tags t")
    List<Object[]> findAllCourseTagPairs();

    @Query("SELECT c FROM Course c WHERE c.id IN :ids AND c.status = 'ACTIVE'")
    List<Course> findAllActiveByIds(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query("UPDATE Course c SET c.totalLessonsCount = c.totalLessonsCount + 1 WHERE c.id = :courseId")
    void incrementLessonCount(@Param("courseId") Long courseId);

    @Modifying
    @Query("UPDATE Course c SET c.totalLessonsCount = c.totalLessonsCount - 1 WHERE c.id = :courseId AND c.totalLessonsCount > 0")
    void decrementLessonCount(@Param("courseId") Long courseId);
    long countByTeacherId(Long teacherId);

    @Query("SELECT c.id, c.name FROM Course c WHERE c.teacher.id = :teacherId")
    List<Object[]> findBasicInfoByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT c FROM Course c " +
           "LEFT JOIN Enrollment e ON c = e.course " +
           "WHERE c.status = 'ACTIVE' " +
           "GROUP BY c " +
           "ORDER BY COUNT(e) DESC")
    List<Course> findTop6MostPurchasedCourses(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c.id, c.name, c.teacher.id FROM Course c")
    List<Object[]> findAllCoursesBasicInfo();
}
