package com.echill.repository;

import com.echill.entity.UserSkillProfile;
import com.echill.entity.enums.MasteryLevel;
import com.echill.entity.enums.TagGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserSkillProfileRepository extends JpaRepository<UserSkillProfile, Long> {
    List<UserSkillProfile> findByUserIdAndTagIdIn(Long userId, Collection<Long> tagIds);
//
//    @Query("SELECT usp FROM UserSkillProfile usp JOIN FETCH usp.tag t " +
//            "WHERE usp.user.id = :userId AND t.tagGroup = :tagGroup " +
//            "ORDER BY usp.proficiencyPercentage ASC")
//    List<UserSkillProfile> findByUserIdAndTagGroup(
//            @Param("userId") Long userId,
//            @Param("tagGroup") TagGroup tagGroup
//    );

    // Tìm profile của 1 user theo Tag cụ thể
    Optional<UserSkillProfile> findByUserIdAndTagId(Long userId, Long tagId);

    // Lấy toàn bộ Tag Con của 1 user thuộc 1 Tag Cha cụ thể
    List<UserSkillProfile> findAllByUserIdAndTagParentId(Long userId, Long parentTagId);

    // Dùng cho thuật toán Recommend: Lấy các kỹ năng đang yếu
    List<UserSkillProfile> findByUserIdAndMasteryLevelIn(Long userId, List<MasteryLevel> levels);

    List<UserSkillProfile> findAllByUserIdAndTagIdIn(Long userId, List<Long> tagIds);

    // 1. Kéo nhiều Profile cùng lúc để tránh N+1 trong Listener
    @Query("SELECT p FROM UserSkillProfile p JOIN FETCH p.tag t LEFT JOIN FETCH t.parent WHERE p.user.id = :userId AND p.tag.id IN :tagIds")
    List<UserSkillProfile> findAllByUserIdAndTagIdIn(@Param("userId") Long userId, @Param("tagIds") Set<Long> tagIds);

    // 1. Kéo TẤT CẢ Profile Con của NHIỀU Tag Cha cùng lúc
    @Query("SELECT p FROM UserSkillProfile p JOIN FETCH p.tag t JOIN FETCH t.parent WHERE p.user.id = :userId AND t.parent.id IN :parentTagIds")
    List<UserSkillProfile> findAllChildProfilesByParentIds(@Param("userId") Long userId, @Param("parentTagIds") Set<Long> parentTagIds);

    // 2. Kéo TẤT CẢ Profile Cha hiện có để update
    @Query("SELECT p FROM UserSkillProfile p JOIN FETCH p.tag t WHERE p.user.id = :userId AND t.id IN :parentTagIds")
    List<UserSkillProfile> findParentProfilesByIds(@Param("userId") Long userId, @Param("parentTagIds") Set<Long> parentTagIds);
}
