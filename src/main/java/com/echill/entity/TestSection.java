package com.echill.entity;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestSection extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @Column(nullable = false, length = 255)
    String title;

    @Column(nullable = false, name = "order_index")
    Integer orderIndex;

    @Column(columnDefinition = "TEXT")
    String instructions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Test test;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<QuestionGroup> questionGroups = new ArrayList<>();

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    List<Question> questions = new ArrayList<>();

    public void addQuestion(Question question) {
        questions.add(question);
        question.setSection(this);
    }

    public void removeQuestion(Question question) {
        questions.remove(question);
        question.setSection(null);
    }

    public void addQuestionGroup(QuestionGroup group) {
        questionGroups.add(group);
        group.setSection(this);
    }

    public void removeQuestionGroup(QuestionGroup group) {
        questionGroups.remove(group);
        group.setSection(null);
    }
}
