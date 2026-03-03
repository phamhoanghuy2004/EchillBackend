package com.echill.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "test_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestResult extends  BaseEntity {
    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    @Column(nullable = false)
    Double score;

    @Column(nullable = false, name = "time_taken_seconds")
    Integer timeTakenSeconds;

    @Column(name = "is_passed", nullable = false)
    @Builder.Default
    Boolean isPassed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // Xóa User -> Quét sạch lịch sử thi của người đó
    User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // Xóa bài Test -> Xóa sạch mọi kết quả thi của bài đó
    Test test;

    public void evaluateResult(Double targetPassScore) {
        if (this.score == null) {
            this.score = 0.0;
        }
        // Tự động quyết định Đậu hay Rớt
        this.isPassed = this.score >= targetPassScore;
    }
}
