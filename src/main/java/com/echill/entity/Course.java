package com.echill.entity;


import com.echill.entity.enums.Level;
import com.echill.entity.enums.Status;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Course extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @Column(nullable = false, length = 200)
    String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    String description;

    @Column(nullable = false, precision = 12, scale = 0)
    BigDecimal price;

    @Column(name = "original_price", precision = 12, scale = 0)
    BigDecimal originalPrice;

    @Column(name = "image_url", length = 1000)
    String imageUrl;

    @Column(name = "image_public_id", length = 1000)
    String imagePublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    Level level = Level.BEGINNER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Lesson> lessons = new ArrayList<>();

    @Column(name = "total_lessons_count", nullable = false)
    @Builder.Default
    Integer totalLessonsCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    User teacher;

    @ManyToMany(fetch = FetchType.LAZY)
    @Builder.Default
    @JoinTable(
            name = "course_tags",
            joinColumns = @JoinColumn(
                    name = "course_id",
                    foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "tag_id",
                    foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE")
            )
    )
    Set<Tag> tags = new HashSet<>();

    public void addTag(Tag tag) {
        if (tag != null) {
            this.tags.add(tag);
        }
    }

    public void removeTag(Tag tag) {
        if (tag != null) {
            this.tags.remove(tag);
        }
    }

    public void clearTags() {
        this.tags.clear();
    }

    public void addLesson(Lesson lesson) {
        lessons.add(lesson);
        lesson.setCourse(this);
    }

    public void removeLesson(Lesson lesson) {
        lessons.remove(lesson);
        lesson.setCourse(null);
    }


    @Transient
    public Integer getDiscountPercent() {
        // Nếu không có giá gốc, hoặc giá gốc = 0, hoặc giá bán >= giá gốc -> KHÔNG GIẢM GIÁ (0%)
        if (this.originalPrice == null
                || this.originalPrice.compareTo(BigDecimal.ZERO) == 0
                || this.price.compareTo(this.originalPrice) >= 0) {
            return 0;
        }

        // Công thức: ((Giá gốc - Giá bán) / Giá gốc) * 100
        BigDecimal discountAmount = this.originalPrice.subtract(this.price);
        BigDecimal percentage = discountAmount.divide(this.originalPrice, 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        return percentage.intValue();
    }

}
