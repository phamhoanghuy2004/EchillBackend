package com.echill.entity;

import com.echill.entity.enums.FileType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Document extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @Column(nullable = false, length = 100)
    String title;

    @Column(nullable = false, name = "file_url", length = 1000)
    String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "file_type", length = 20)
    @Builder.Default
    FileType fileType = FileType.PDF;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Lesson lesson;

}
