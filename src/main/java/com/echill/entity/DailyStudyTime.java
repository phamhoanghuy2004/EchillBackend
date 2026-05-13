package com.echill.entity;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "daily_study_times",
        // Đảm bảo 1 user mỗi ngày chỉ có 1 record duy nhất
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "study_date"})},
        // Index để query theo tuần cực nhanh
        indexes = {@Index(name = "idx_user_study_date", columnList = "user_id, study_date")}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DailyStudyTime {
    @Id
    @Tsid
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "study_date", nullable = false)
    LocalDate studyDate; // Lưu theo ngày (VD: 2023-10-25)

    @Column(name = "total_seconds", nullable = false)
    @Builder.Default
    Long totalSeconds = 0L; // Tổng thời gian học trong ngày đó

    // Cập nhật Atomic (Nguyên tử) trên RAM trước khi save
    public void addSeconds(Long seconds) {
        if (seconds != null && seconds > 0) {
            this.totalSeconds += seconds;
        }
    }
}
