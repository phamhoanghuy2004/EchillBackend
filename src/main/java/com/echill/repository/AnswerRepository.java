package com.echill.repository;

import com.echill.entity.Answer;
import com.echill.repository.projection.CorrectAnswerProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByQuestionId(Long questionId);

    // TỐI ƯU SỐ 1: Chỉ lấy đúng ID của Câu hỏi và ID của Đáp án đúng. Siêu nhẹ!
    @Query("SELECT q.id AS questionId, a.id AS answerId " +
            "FROM Answer a JOIN a.question q JOIN q.section s " +
            "WHERE s.test.id = :testId AND a.isCorrect = true")
    List<CorrectAnswerProjection> findCorrectAnswersByTestId(@Param("testId") Long testId);
}
