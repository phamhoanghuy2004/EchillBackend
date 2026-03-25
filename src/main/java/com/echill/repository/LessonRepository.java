package com.echill.repository;

import com.echill.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("SELECT DISTINCT l FROM Lesson l " +
            "JOIN FETCH l.course " +
            "LEFT JOIN FETCH l.documents " +
            "WHERE l.id = :lessonId")
    Optional<Lesson> findByIdWithCourseAndDocuments(@Param("lessonId") Long lessonId);

    Optional<Lesson> findByPublicVideoId(String publicVideoId);
}
