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
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    User user;

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

    @Column(name = "image_public_id")
    String imagePublicId;

    public void updateScores(Double listening, Double reading, Double speaking, Double writing) {
        this.listeningScore = listening != null ? listening : 0.0;
        this.readingScore = reading != null ? reading : 0.0;
        this.speakingScore = speaking != null ? speaking : 0.0;
        this.writingScore = writing != null ? writing : 0.0;

        if (this.certType == CertType.IELTS) {
            double average = (this.listeningScore + this.readingScore + this.speakingScore + this.writingScore) / 4.0;
            this.totalScore = Math.ceil(average * 2) / 2.0;
        } else {
            this.totalScore = this.listeningScore + this.readingScore + this.speakingScore + this.writingScore;
        }
    }

}
