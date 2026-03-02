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
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Question extends BaseEntity {
    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(name = "audio_url")
    String audioUrl;

    @Column(name = "image_url")
    String imageUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    String explanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // Bài Test bị xóa -> Câu hỏi bốc hơi theo
    Test test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tag_id",
            nullable = true, // Quan trọng: Cho phép câu hỏi không gắn tag
            // Xóa Tag -> Cột tag_id của câu hỏi này tự động biến thành NULL
            foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE SET NULL")
    )
    Tag tag;
}
