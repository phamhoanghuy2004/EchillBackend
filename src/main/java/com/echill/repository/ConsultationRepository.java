package com.echill.repository;

import com.echill.entity.Consultation;
import com.echill.entity.TestSet;
import com.echill.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long>, JpaSpecificationExecutor<Consultation> {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Consultation c SET c.handledBy = :admin, c.status = 'IN_PROGRESS' " +
            "WHERE c.id = :requestId AND c.status = 'PENDING'")
    int assignConsultation(@Param("requestId") Long requestId, @Param("admin") User admin);

    Optional<Consultation> findByEmail(String email);
}
