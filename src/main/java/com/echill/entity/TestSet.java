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
@Table(name = "test_sets", uniqueConstraints = {
        @UniqueConstraint(
                columnNames = {"lesson_id"}
        )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestSet extends BaseEntity {
    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    @Column(nullable = false, length = 100)
    String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    String description;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    Boolean isPublic = true;

    Integer year;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id") // Cho phép NULL (Dành cho bộ đề độc lập)
    @OnDelete(action = OnDeleteAction.CASCADE) // Xóa Lesson -> Xóa cmn luôn Bộ đề này
    Lesson lesson;
}
