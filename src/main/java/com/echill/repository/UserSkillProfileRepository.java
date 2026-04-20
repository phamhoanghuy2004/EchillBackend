package com.echill.repository;

import com.echill.entity.UserSkillProfile;
import com.echill.entity.enums.TagGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSkillProfileRepository extends JpaRepository<UserSkillProfile, Long> {
    List<UserSkillProfile> findByUserIdAndTagIdIn(Long userId, Collection<Long> tagIds);

    @Query("SELECT usp FROM UserSkillProfile usp JOIN FETCH usp.tag t " +
            "WHERE usp.user.id = :userId AND t.tagGroup = :tagGroup " +
            "ORDER BY usp.proficiencyPercentage ASC")
    List<UserSkillProfile> findByUserIdAndTagGroup(
            @Param("userId") Long userId,
            @Param("tagGroup") TagGroup tagGroup
    );
}
