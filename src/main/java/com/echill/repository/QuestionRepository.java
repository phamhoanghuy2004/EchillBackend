package com.echill.repository;

import com.echill.entity.Question;
import com.echill.entity.enums.SkillType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("SELECT q FROM Question q " +
            "JOIN FETCH q.section s " +
            "JOIN FETCH s.test t " +
            "JOIN FETCH t.testSet ts " +
            "JOIN FETCH ts.user u " +
            "LEFT JOIN FETCH q.answers a " +
            "WHERE q.id = :questionId")
    Optional<Question> findByIdWithFullRelations(@Param("questionId") Long questionId);

    @Query("SELECT q FROM Question q " +
            "JOIN FETCH q.section s " +
            "JOIN FETCH s.test t " +
            "JOIN FETCH t.testSet ts " +
            "LEFT JOIN FETCH ts.lesson l " +
            "LEFT JOIN FETCH q.answers " +
            "LEFT JOIN FETCH q.tag " +
            "WHERE q.id = :questionId")
    Optional<Question> findByIdForChat(@Param("questionId") Long questionId);

    @Query("SELECT q FROM Question q WHERE q.section.test.id = :testId")
    List<Question> findByTestId(@Param("testId") Long testId);

    @Query("SELECT COUNT(q) FROM Question q JOIN q.section s WHERE s.test.id = :testId")
    Integer countTotalQuestionsByTestId(@Param("testId") Long testId);

//    @Query("SELECT q.id FROM Question q JOIN q.tags t " +
//            "WHERE t.parent.id = :parentTagId " +
//            "AND q.difficultyLevel = :level " +
//            "AND q.id NOT IN :excludedIds")
//    List<Long> findEligibleQuestionIds(
//            @Param("parentTagId") Long parentTagId,
//            @Param("level") Integer level,
//            @Param("excludedIds") Set<Long> excludedIds
//    );

    @Query("SELECT DISTINCT q FROM Question q " +
            "JOIN FETCH q.answers " +
            "LEFT JOIN FETCH q.tag t " +      // 🟢 Đã sửa thành q.tag và dùng LEFT JOIN
            "LEFT JOIN FETCH t.parent " +     // 🟢 Chống N+1 cho Parent Tag
            "JOIN q.section s " +
            "JOIN s.test test " +
            "WHERE test.type = 'PLACEMENT_TEST'")
    List<Question> findAllPlacementTestQuestions();

    @Query("SELECT DISTINCT q FROM Question q " +
            "JOIN FETCH q.answers " +
            "LEFT JOIN FETCH q.questionGroup " +
            "WHERE q.tag.id = :tagId " +
            "AND q.skillType = :skillType " +
            "AND q.difficultyLevel = :difficulty")
    List<Question> findListeningQuestionsByTagAndDifficulty(
            @Param("tagId") Long tagId,
            @Param("skillType") SkillType skillType,
            @Param("difficulty") Integer difficulty);

    @Query("SELECT DISTINCT q FROM Question q " +
            "JOIN FETCH q.answers " +
            "LEFT JOIN FETCH q.questionGroup " +
            "WHERE q.tag.id = :tagId " +
            "AND q.skillType = :skillType")
    List<Question> findListeningQuestionsByTag(
            @Param("tagId") Long tagId,
            @Param("skillType") SkillType skillType);

    boolean existsByTagIdAndSkillType(Long tagId, SkillType skillType);

    @Query("SELECT DISTINCT q FROM Question q " +
            "JOIN FETCH q.answers " +
            "LEFT JOIN FETCH q.questionGroup " +
            "WHERE q.id = :questionId")
    Optional<Question> findByIdForClone(@Param("questionId") Long questionId);

    @Query("SELECT q FROM Question q WHERE q.questionGroup.id = :groupId ORDER BY q.orderIndex ASC")
    List<Question> findByQuestionGroupIdOrderByOrderIndex(@Param("groupId") Long groupId);

    @Query("SELECT DISTINCT q FROM Question q JOIN FETCH q.answers WHERE q.id IN :questionIds")
    List<Question> findByIdsWithAnswers(@Param("questionIds") List<Long> questionIds);
}
