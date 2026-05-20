package com.echill.entity;

import com.echill.entity.enums.SkillType;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "questions", indexes = {
        @Index(name = "idx_question_difficulty", columnList = "difficulty_level")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Question extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(name = "audio_url", length = 1000)
    String audioUrl;

    @Column(name = "audio_public_id")
    String audioPublicId;

    @Column(name = "image_url", length = 1000)
    String imageUrl;

    @Column(name = "image_public_id")
    String imagePublicId;

    @Column(columnDefinition = "TEXT")
    String explanation;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", length = 20)
    SkillType skillType;

    @Column(nullable = false, name = "order_index")
    Integer orderIndex;

    @Column(name = "difficulty_level", nullable = false)
    @Builder.Default
    Integer difficultyLevel = 3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    TestSection section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    QuestionGroup questionGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tag_id",
            foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE SET NULL")
    )
    Tag tag;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 10)
    @Builder.Default
    List<Answer> answers = new ArrayList<>();

    public void addAnswer(Answer answer) {
        answers.add(answer);
        answer.setQuestion(this);
    }

    public void removeAnswer(Answer answer) {
        answers.remove(answer);
        answer.setQuestion(null);
    }
}