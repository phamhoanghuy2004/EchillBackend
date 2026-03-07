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

    @Column(nullable = false, length = 100)
    String title;

    @Column(nullable = false, name = "duration_minutes")
    Integer durationMinutes;

    @Column(nullable = false, name = "pass_score")
    Double passScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_set_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // Xóa Bộ đề -> Xóa luôn các Đề thi bên trong
    TestSet testSet;
}
