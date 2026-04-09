package com.echill.entity;

import com.echill.entity.enums.TestType;
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
@Table(name = "tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Test extends BaseEntity  {
    @Id
    @Tsid
    Long id;

    @Column(nullable = false, length = 255)
    String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", length = 20, nullable = false)
    @Builder.Default
    TestType type = TestType.PRACTICE;

    @Column(nullable = false, name = "duration_minutes")
    Integer durationMinutes;

    @Column(nullable = false, name = "pass_score")
    @Builder.Default
    Double passScore = 0.0;  // Passcore này giờ tính dựa trên tỉ lệ

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_set_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    TestSet testSet;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<TestSection> sections = new ArrayList<>();

    public void addSection(TestSection section) {
        sections.add(section);
        section.setTest(this);
    }

    public void removeSection(TestSection section) {
        sections.remove(section);
        section.setTest(null);
    }
}
