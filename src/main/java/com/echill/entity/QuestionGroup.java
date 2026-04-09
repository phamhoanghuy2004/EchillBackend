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
@Table(name = "question_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionGroup extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @Column(name = "shared_content", columnDefinition = "TEXT")
    String sharedContent;

    @Column(name = "shared_audio_url", length = 1000)
    String sharedAudioUrl;

    @Column(name = "shared_image_url", length = 1000)
    String sharedImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    TestSection section;

    @OneToMany(mappedBy = "questionGroup", cascade = CascadeType.ALL)
    @Builder.Default
    List<Question> questions = new ArrayList<>();

    public void addQuestion(Question question) {
        questions.add(question);
        question.setQuestionGroup(this);
    }

    public void removeQuestion(Question question) {
        questions.remove(question);
        question.setQuestionGroup(null);
    }
}
