package com.echill.entity;

import com.echill.entity.enums.CertType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Certificate extends BaseEntity {
    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_profile_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // Bổ sung dòng này để MySQL tự dọn dẹp khi xóa Teacher
    TeacherProfile teacherProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "cert_type", nullable = false, length = 20)
    CertType certType;

    @Column(name = "total_score", nullable = false)
    Double totalScore;

    @Column(name = "listening_score")
    Double listeningScore;

    @Column(name = "reading_score")
    Double readingScore;

    @Column(name = "speaking_score")
    Double speakingScore;

    @Column(name = "writing_score")
    Double writingScore;

    @Column(name = "issued_date", nullable = false)
    LocalDate issuedDate;

    @Column(name = "evidence_url", nullable = false)
    String evidenceUrl;

}
