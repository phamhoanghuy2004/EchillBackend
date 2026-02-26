package com.echill.entity;

import com.echill.entity.enums.CertType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;


@Entity
@Table(name = "study_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudyGoal extends BaseEntity {
    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    StudentProfile studentProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "cert_type", nullable = false, length = 20)
    CertType certType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;

    @Column(name = "target_listening")
    Double targetListening;

    @Column(name = "target_reading")
    Double targetReading;

    @Column(name = "target_speaking")
    Double targetSpeaking;

    @Column(name = "target_writing")
    Double targetWriting;

    @Column(name = "target_total")
    Double targetTotal;

    @Column(name = "current_listening")
    Double currentListening;

    @Column(name = "current_reading")
    Double currentReading;

    @Column(name = "current_speaking")
    Double currentSpeaking;

    @Column(name = "current_writing")
    Double currentWriting;

    @Column(name = "current_total")
    Double currentTotal;

}
