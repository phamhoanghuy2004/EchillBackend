package com.echill.repository;

import com.echill.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate,Long> {
    List<Certificate> findByTeacherProfileId(Long teacherProfileId);

    List<Certificate> findAllByTeacherProfileIdIn(List<Long> teacherProfileIds);


    @Query("SELECT c.imagePublicId FROM Certificate c WHERE c.imagePublicId IS NOT NULL")
    List<String> findAllImagePublicIds();
}
