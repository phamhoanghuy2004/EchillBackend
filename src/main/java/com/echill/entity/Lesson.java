package com.echill.entity;

import com.echill.entity.enums.VideoStatus;
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
@Table(name = "lessons",indexes = {
        @Index(name = "idx_public_video_id", columnList = "public_video_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Lesson extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @Column(nullable = false, length = 200)
    String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(nullable = false, name = "display_order")
    Integer displayOrder;

    @Column(nullable = false, name = "is_preview")
    @Builder.Default
    Boolean isPreview = false;

    @Column(name = "public_video_id", length = 1000, unique = true)
    String publicVideoId; // ID trên Cloudinary

    @Column(name = "raw_url", length = 1000)
    String rawUrl;   // Link MP4 gốc, set lúc frontend gọi về

    @Column(name = "hls_url", length = 1000)
    String hlsUrl;   // Link m3u8 (Sẽ có sau khi webhook gọi về)

    @Enumerated(EnumType.STRING)
    @Builder.Default
    VideoStatus videoStatus = VideoStatus.NONE;

    @Column(name = "duration_seconds")
    @Builder.Default
    Long durationSeconds = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Course course;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Document> documents = new ArrayList<>();

    public void addDocument(Document document) {
        documents.add(document);
        document.setLesson(this);
    }

    public void removeDocument(Document document) {
        documents.remove(document);
        document.setLesson(null);
    }
}
