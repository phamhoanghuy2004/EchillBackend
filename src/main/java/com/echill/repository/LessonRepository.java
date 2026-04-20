package com.echill.repository;

import com.echill.entity.Lesson;
import com.echill.entity.enums.VideoStatus;
import com.echill.repository.projection.LessonWithProgressProjection;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("SELECT DISTINCT l FROM Lesson l " +
            "JOIN FETCH l.course c " +
            "JOIN FETCH c.teacher " +
            "LEFT JOIN FETCH l.documents " +
            "WHERE l.id = :lessonId")
    Optional<Lesson> findByIdWithCourseAndTeacherAndDocuments(@Param("lessonId") Long lessonId);

    @Query("SELECT l FROM Lesson l " +
            "JOIN FETCH l.course c " +
            "JOIN FETCH c.teacher " +
            "WHERE l.id = :lessonId")
    Optional<Lesson> findByIdForOwnershipCheck(@Param("lessonId") Long lessonId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    Optional<Lesson> findByPublicVideoId(String publicVideoId);

    @Query("SELECT l.publicVideoId FROM Lesson l WHERE l.publicVideoId IS NOT NULL")
    List<String> findAllVideoPublicIds();

    @Query("SELECT l FROM Lesson l WHERE l.videoStatus = :status AND l.updatedAt < :threshold")
    List<Lesson> findStuckLessons(@Param("status") VideoStatus status, @Param("threshold") LocalDateTime threshold);

    @Query("""
        SELECT l.id AS lessonId,
               l.title AS title,
               l.displayOrder AS displayOrder,
               l.durationSeconds AS durationSeconds,
               l.videoStatus AS videoStatus,
               l.version AS lessonVersion,
               (CASE WHEN EXISTS (SELECT d.id FROM Document d WHERE d.lesson.id = l.id) THEN true ELSE false END) AS hasDocument,
               (CASE WHEN EXISTS (SELECT t.id FROM TestSet t WHERE t.lesson.id = l.id) THEN true ELSE false END) AS hasTest,
               lp.id AS progressId,
               lp.isCompleted AS isCompleted,
               lp.versionCompleted AS versionCompleted,
               COALESCE(lp.lastWatchedSecond, 0) AS lastWatchedSecond 
        FROM Lesson l
        LEFT JOIN LessonProgress lp ON lp.lesson.id = l.id AND lp.enrollment.id = :enrollmentId
        WHERE l.course.id = :courseId
        ORDER BY l.displayOrder ASC
    """)
    List<LessonWithProgressProjection> findLessonsWithProgress(
            @Param("courseId") Long courseId,
            @Param("enrollmentId") Long enrollmentId
    );

    @Query("""
        SELECT CASE WHEN EXISTS (
            SELECT l.id 
            FROM Lesson l
            LEFT JOIN LessonProgress lp ON lp.lesson.id = l.id AND lp.enrollment.id = :enrollmentId
            WHERE l.course.id = :courseId
              AND l.displayOrder < :currentDisplayOrder
              AND (lp.id IS NULL OR lp.isCompleted = false)
        ) THEN true ELSE false END
    """)
    boolean existsUncompletedPreviousLessons(
            @Param("courseId") Long courseId,
            @Param("enrollmentId") Long enrollmentId,
            @Param("currentDisplayOrder") Integer currentDisplayOrder
    );

    @Query("""
        SELECT DISTINCT l FROM Lesson l 
        LEFT JOIN FETCH l.documents 
        LEFT JOIN FETCH l.testSet 
        WHERE l.id = :lessonId
    """)
    Optional<Lesson> findLessonWithDetailsById(@Param("lessonId") Long lessonId);
}
