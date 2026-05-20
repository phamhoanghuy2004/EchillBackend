package com.echill.repository;

import com.echill.entity.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {
    @Query("SELECT tp FROM TeacherProfile tp JOIN FETCH tp.user")
    List<TeacherProfile> findAllWithUser();

    @Query(value = "SELECT * FROM teacher_profiles ORDER BY RAND()", nativeQuery = true)
    List<TeacherProfile> findRandomTeachers(org.springframework.data.domain.Pageable pageable);
}
