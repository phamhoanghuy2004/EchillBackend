package com.echill.entity;

import com.echill.entity.enums.TestSessionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_sessions", indexes = {
        @Index(name = "idx_session_composite", columnList = "student_id, test_set_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestSession extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long studentId;

    Long testSetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    Test test;

    LocalDateTime startTime;

    LocalDateTime endTime;

    @Column(name = "test_snapshot", columnDefinition = "LONGTEXT", nullable = false)
    String testSnapshot;

    @Enumerated(EnumType.STRING)
    TestSessionStatus status;

    @Column(name = "active_lock", unique = true)
    String activeLock;
}
