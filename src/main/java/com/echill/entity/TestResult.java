package com.echill.entity;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "test_results", indexes = {
        @Index(name = "idx_test_result_student", columnList = "student_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestResult extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @Column(name = "listening_score")
    @Builder.Default
    Double listeningScore = 0.0;

    @Column(name = "reading_score")
    @Builder.Default
    Double readingScore = 0.0;

    @Column(name = "speaking_score")
    @Builder.Default
    Double speakingScore = 0.0;

    @Column(name = "writing_score")
    @Builder.Default
    Double writingScore = 0.0;

    @Column(name = "total_score", nullable = false)
    @Builder.Default
    Double totalScore = 0.0;

    @Column(nullable = false, name = "time_taken_seconds")
    Integer timeTakenSeconds;

    @Column(name = "is_passed", nullable = false)
    @Builder.Default
    Boolean isPassed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Test test;

    public void evaluateResult(Double targetPassScore) {
        if (this.totalScore == null) {
            this.totalScore = 0.0;
        }
        this.isPassed = this.totalScore >= targetPassScore;
    }
}