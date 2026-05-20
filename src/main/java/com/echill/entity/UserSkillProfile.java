package com.echill.entity;

import com.echill.entity.enums.MasteryLevel;
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
        },
        indexes = {
                @Index(name = "idx_mastery_level", columnList = "mastery_level")
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

    @Column(name = "current_level", nullable = false)
    @Builder.Default
    Integer currentLevel = 0;

    @Column(name = "is_inferred", nullable = false)
    @Builder.Default
    Boolean isInferred = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "mastery_level", length = 30, nullable = false)
    @Builder.Default
    MasteryLevel masteryLevel = MasteryLevel.BEGINNER;

    @Column(name = "last_tested_at", nullable = false)
    @Builder.Default
    LocalDateTime lastTestedAt = LocalDateTime.now();

    /**
     * Hàm tự động tính toán MasteryLevel dựa trên max_level của Tag
     * Gọi hàm này mỗi khi update currentLevel.
     */
    public void calculateMasteryLevel() {
        if (this.tag == null || this.tag.getMaxLevel() == null || this.tag.getMaxLevel() == 0) {
            this.masteryLevel = MasteryLevel.BEGINNER;
            return;
        }

        // Ép currentLevel không được vượt quá maxLevel của Tag
        int cappedLevel = Math.min(this.currentLevel, this.tag.getMaxLevel());
        double ratio = (double) cappedLevel / this.tag.getMaxLevel();

        if (ratio <= 0.25) {
            this.masteryLevel = MasteryLevel.BEGINNER;
        } else if (ratio <= 0.50) {
            this.masteryLevel = MasteryLevel.INTERMEDIATE;
        } else if (ratio <= 0.75) {
            this.masteryLevel = MasteryLevel.ADVANCED;
        } else {
            this.masteryLevel = MasteryLevel.MASTER;
        }
    }

    /**
     * Helper method cập nhật nhanh
     */
    public void updateSkill(Integer newLevel, boolean inferred) {
        this.currentLevel = newLevel;
        this.isInferred = inferred;
        this.lastTestedAt = LocalDateTime.now();
        this.calculateMasteryLevel();
    }
}