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
@Table(name = "answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Answer extends BaseEntity {

    @Id
    @Tsid
    Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content; // Nội dung đáp án (Ví dụ: "He is running")

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    Boolean isCorrect = false; // Đánh dấu đây là đáp án đúng

    // --- LIÊN KẾT VỚI CÂU HỎI (SỐNG CÒN) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // Câu hỏi bị xóa -> 4 đáp án đi tong
            Question question;
}
