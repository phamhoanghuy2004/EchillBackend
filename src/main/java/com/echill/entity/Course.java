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
import java.util.HashSet;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    @Column(nullable = false, length = 200)
    String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    String description;

    @Column(nullable = false, precision = 12, scale = 0)
    BigDecimal price;

    @Column(nullable = false, name = "image_url")
    String imageUrl;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    User teacher;

    @ManyToMany(fetch = FetchType.LAZY)
    @Builder.Default
    @JoinTable(
            name = "course_tags",
            joinColumns = @JoinColumn(
                    name = "course_id",
                    // Xóa Course -> Tự động xóa dòng liên kết trong course_tags
                    foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "tag_id",
                    // Xóa Tag -> Tự động xóa dòng liên kết trong course_tags
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

}
