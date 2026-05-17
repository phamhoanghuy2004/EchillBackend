package com.echill.repository;

import com.echill.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate,Long> {
    List<Certificate> findByUserId(Long userId);

    List<Certificate> findAllByUserIdIn(List<Long> userIds);


    @Query("SELECT c.imagePublicId FROM Certificate c WHERE c.imagePublicId IS NOT NULL")
    List<String> findAllImagePublicIds();

    @Query("""
            SELECT c FROM Certificate c
            JOIN FETCH c.user u
            JOIN u.userRoles ur
            JOIN ur.role r
            WHERE c.certType = :certType
              AND r.name = 'STUDENT'
            ORDER BY c.totalScore DESC
            """)
    List<Certificate> findTopCertificates(@org.springframework.data.repository.query.Param("certType") com.echill.entity.enums.CertType certType, org.springframework.data.domain.Pageable pageable);
}
