package com.echill.entity;

import com.echill.entity.enums.ConsultationStatus;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "consultations", indexes = {
        @Index(name = "idx_status_created_at", columnList = "status, created_at"),

        @Index(name = "idx_admin_created_at", columnList = "admin_id, created_at"),

        @Index(name = "idx_consultation_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Consultation extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @Column(nullable = false) String fullName;
    @Column(nullable = false, unique = true) String email;
    @Column(nullable = false) String phoneNumber;
    Integer birthYear;
    String topic;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    ConsultationStatus status = ConsultationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    User handledBy;
}
