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
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
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

    // 1. Cập nhật tiến độ điểm hiện tại (Dành cho TOEIC Nói & Viết hoặc 4 kỹ năng)
    public void updateCurrentProgress(Double listening, Double reading, Double speaking, Double writing, Double total) {
        // Có thể áp dụng validation ở đây: Điểm không được số âm
        if (listening != null && listening < 0) throw new IllegalArgumentException("Điểm không hợp lệ");

        this.currentListening = listening;
        this.currentReading = reading;
        this.currentSpeaking = speaking;
        this.currentWriting = writing;
        this.currentTotal = total;
    }

    // 2. Đánh dấu mục tiêu này đã hoàn thành hoặc bị bỏ ngang (Lưu trữ lịch sử)
    public void markAsInactive() {
        this.isActive = false;
    }

}
