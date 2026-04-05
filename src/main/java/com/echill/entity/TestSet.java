package com.echill.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    Long id;

    @Column(nullable = false, length = 100)
    String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    String description;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    Boolean isPublic = true;

    Integer year;

    @OneToMany(mappedBy = "testSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Test>  tests = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    User user;

    public void addTest(Test test) {
        tests.add(test);
        test.setTestSet(this);
    }

    public void removeTest(Test test) {
        tests.remove(test);
        test.setTestSet(null);
    }
}
