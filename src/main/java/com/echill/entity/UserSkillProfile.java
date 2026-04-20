package com.echill.entity;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_skill_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_tag", columnNames = {"user_id", "tag_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserSkillProfile extends BaseEntity {

    @Id
    @Tsid
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    Tag tag;

    @Column(name = "proficiency_percentage", nullable = false)
    @Builder.Default
    Double proficiencyPercentage = 0.0;

    @Column(name = "last_tested_at", nullable = false)
    @Builder.Default
    LocalDateTime lastTestedAt = LocalDateTime.now();

    @Column(name = "latest_delta", nullable = false)
    @Builder.Default
    Double latestDelta = 0.0;

    public void updateProficiency(Double newScore) {
        Double cappedNewScore = Math.max(0.0, Math.min(100.0, newScore));

        // Tính độ lệch: Điểm mới - Điểm cũ
        this.latestDelta = cappedNewScore - this.proficiencyPercentage;

        this.proficiencyPercentage = cappedNewScore;
        this.lastTestedAt = LocalDateTime.now();
    }
}