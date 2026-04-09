package com.echill.job;

import com.echill.entity.Lesson;
import com.echill.entity.enums.VideoStatus;
import com.echill.repository.LessonRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VideoCleanupTask {
    LessonRepository lessonRepository;

    /**
     * 💥 Chạy mỗi 30 phút để giải phóng các Lesson bị kẹt ở trạng thái PROCESSING.
     * fixedRate = 1800000 ms (30 phút)
     */
    @Scheduled(fixedRate = 1800000)
    @Transactional
    public void releaseStuckProcessingLessons() {
        log.info("🧹 Cron Job: Bắt đầu quét các bài học bị kẹt trạng thái PROCESSING...");

        // Ngưỡng thời gian: 2 tiếng trước
        LocalDateTime threshold = LocalDateTime.now().minusHours(2);

        List<Lesson> stuckLessons = lessonRepository.findStuckLessons(VideoStatus.PROCESSING, threshold);

        if (stuckLessons.isEmpty()) {
            log.info("✅ Không có bài học nào bị kẹt. Kết thúc task.");
            return;
        }

        log.warn("🚨 Phát hiện {} bài học bị kẹt quá 2 giờ. Tiến hành đưa về FAILED để giải phóng khóa!", stuckLessons.size());

        for (Lesson lesson : stuckLessons) {
            lesson.failVideoProcessing();
            log.info("   -> Đã giải phóng Lesson ID: {} (PublicId cũ: {})", lesson.getId(), lesson.getPublicVideoId());
        }

        lessonRepository.saveAll(stuckLessons);
        log.info("🧹 Hoàn tất dọn dẹp bài học bị kẹt.");
    }
}
