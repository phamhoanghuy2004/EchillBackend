package com.echill.entity;

import com.echill.entity.enums.VideoStatus;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "lessons",indexes = {
        @Index(name = "idx_public_video_id", columnList = "public_video_id"),
        @Index(name = "idx_lesson_course_order", columnList = "course_id, display_order")
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

    @Column(name = "public_video_id", length = 150, unique = true)
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
    @BatchSize(size = 50)
    @Builder.Default
    Set<Document> documents = new HashSet<>();

    @OneToOne(mappedBy = "lesson", cascade = CascadeType.ALL)
    TestSet testSet;

    @ManyToMany(fetch = FetchType.LAZY)
    @Builder.Default
    @JoinTable(
            name = "lesson_tags",
            joinColumns = @JoinColumn(
                    name = "lesson_id",
                    foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "tag_id",
                    foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE")
            )
    )
    Set<Tag> tags = new HashSet<>();

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 0;

    public void addDocument(Document document) {
        documents.add(document);
        document.setLesson(this);
    }

    public void removeDocument(Document document) {
        documents.remove(document);
        document.setLesson(null);
    }

    public boolean isVideoProcessing() {
        return this.videoStatus == VideoStatus.PROCESSING;
    }

    public boolean isVideoReady() {
        return this.videoStatus == VideoStatus.READY;
    }

    public void failVideoProcessing(){
        this.videoStatus = VideoStatus.FAILED;
        this.publicVideoId = null;
        this.rawUrl = null;
        this.durationSeconds = 0L;
    }

    /**
     * Hành vi: Bắt đầu quá trình tải video mới lên (Ghi nháp)
     */
    public void startVideoProcessing(String newPublicId, String newRawUrl, Long durationSeconds) {
        if (this.isVideoProcessing()) {
            throw new AppException(ErrorEnum.VIDEO_IS_PROCESSING);
        }

        this.publicVideoId = newPublicId;
        this.rawUrl = newRawUrl;
        this.durationSeconds = durationSeconds;
        this.videoStatus = VideoStatus.PROCESSING;
    }

    /**
     * Hành vi: Xác nhận video đã convert xong từ Webhook Cloudinary
     */
    public void finishVideoProcessing(String hlsUrl) {

        if (!this.isVideoProcessing()) {
            return;
        }

        this.hlsUrl = hlsUrl;
        this.videoStatus = VideoStatus.READY;

        // 🟢 NGHIỆP VỤ: Đánh dấu bài học đã có video mới, bắt học viên học lại
        this.incrementVersion();
    }

    public void incrementVersion() {
        this.version++;
    }
}
