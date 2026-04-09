package com.echill.repository;

import com.echill.entity.Lesson;
import com.echill.entity.enums.VideoStatus;
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
}
