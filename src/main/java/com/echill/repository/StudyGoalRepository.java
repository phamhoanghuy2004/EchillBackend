package com.echill.repository;

import com.echill.entity.StudyGoal;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudyGoalRepository extends JpaRepository<StudyGoal, Long> {
    Optional<StudyGoal> findByStudentProfileIdAndIsActiveTrue(Long profile);
    Optional<StudyGoal> findByIdAndStudentProfileIdAndIsActiveTrue(Long id, Long studentProfileId);
}
