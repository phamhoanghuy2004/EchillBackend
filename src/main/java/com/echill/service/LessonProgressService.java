package com.echill.service;

import com.echill.dto.response.learner.VideoCompleteResponse;
import com.echill.entity.LessonProgress;
import com.echill.exception.AppException;
import com.echill.exception.StudentErrorEnum;
import com.echill.repository.LessonProgressRepository;
import com.echill.repository.TestResultRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonProgressService {
    LessonProgressRepository progressRepository;
    StringRedisTemplate redisTemplate;
    TestResultRepository testResultRepository;

    static String PROGRESS_KEY_PREFIX = "progress:data:v2:";

    @Transactional
    public VideoCompleteResponse markVideoAsWatched(Long lessonId) {
        Long userId = SecurityUtils.getCurrentUserId();

        LessonProgress progress = progressRepository.findProgressWithLessonAndTestSet(lessonId, userId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.LESSON_NOT_STARTED));

        Integer currentVersion = progress.getLesson().getVersion();

        if (progress.isValidVideoWatched(currentVersion)) {
            log.debug("♻️ Video bài {} của User {} đã được đánh dấu xem xong từ trước (Version {}).",
                    lessonId, userId, currentVersion);
            return VideoCompleteResponse.builder()
                    .videoWatched(true)
                    .lessonCompleted(progress.isValidCompleted(currentVersion))
                    .build();
        }

        String dataKey = PROGRESS_KEY_PREFIX + lessonId + "_" + userId;
        Object secObj = redisTemplate.opsForHash().get(dataKey, "sec");

        int currentWatchedSeconds = (secObj != null)
                ? Integer.parseInt(secObj.toString())
                : progress.getLastWatchedSecond();

        Long totalDuration = progress.getLesson().getDurationSeconds();

        if (totalDuration == null || totalDuration == 0) {
            log.error("❌ Lỗi thời lượng của bài học có id: {} bằng 0", lessonId);
            throw new AppException(StudentErrorEnum.LESSON_NOT_READY);
        }

        int requiredSeconds = (int) Math.floor(totalDuration * 0.9);
        double percentage = ((double) currentWatchedSeconds / totalDuration) * 100;

        if (currentWatchedSeconds < requiredSeconds) {
            log.warn("🚨 [HACK ATTEMPT] User {} Complete bài {} nhưng mới xem {}s/{}s (Đạt: {}%).",
                    userId, lessonId, currentWatchedSeconds, requiredSeconds, String.format("%.2f", percentage));
            throw new AppException(StudentErrorEnum.NOT_ENOUGH_PROGRESS);
        }

        progress.markVideoWatched();
        log.info("🎬 User {} đã xem xong video bài học {} (Tiến độ: {}%).",
                userId, lessonId, String.format("%.2f", percentage));

        boolean hasTest = progress.getLesson().getTestSet() != null;
        boolean quizAlreadyPassed = progress.isValidQuizPassed(currentVersion);

        if (!hasTest || quizAlreadyPassed) {
            progress.markAsCompleted(currentVersion);
            log.info("🏆 [LESSON COMPLETED] User {} đã hoàn thành trọn vẹn bài học {} (Version: {}).",
                    userId, lessonId, currentVersion);
        } else {
            log.info("⏳ [WAITING QUIZ] User {} đã xong video nhưng cần làm bài tập để hoàn thành bài học {}.",
                    userId, lessonId);
        }

        progressRepository.save(progress);

        return VideoCompleteResponse.builder()
                .videoWatched(true)
                .lessonCompleted(progress.isValidCompleted(currentVersion))
                .build();
    }

    public Long getCompletedLessonsCountThisWeek() {
        Long userId = SecurityUtils.getCurrentUserId();

        // 1. Lấy múi giờ chuẩn VN
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        // 2. Lấy CHÍNH XÁC 00:00:00 của ngày Thứ 2 tuần này
        ZonedDateTime startOfWeek = now.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);

        // 3. Lấy CHÍNH XÁC 23:59:59.999 của ngày Chủ Nhật tuần này
        ZonedDateTime endOfWeek = startOfWeek.plusDays(7).minusNanos(1);

        // 4. Convert ZonedDateTime sang Instant để query vào DB (DB lưu chuẩn UTC)
        return progressRepository.countCompletedLessonsInDateRange(
                userId,
                startOfWeek.toInstant(),
                endOfWeek.toInstant()
        );
    }

    public Long getCompletedTestsCountThisWeek() {
        Long userId = SecurityUtils.getCurrentUserId();

        // 1. Chốt múi giờ Việt Nam
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        // 2. Chặt cụt thời gian về 00:00:00 của Thứ 2
        ZonedDateTime startOfWeek = now.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);

        // 3. Đẩy lên 23:59:59.999 của Chủ Nhật
        ZonedDateTime endOfWeek = startOfWeek.plusDays(7).minusNanos(1);

        // 4. Query Database (Sử dụng createdAt)
        return testResultRepository.countTestsTakenInDateRange(
                userId,
                startOfWeek.toInstant(),
                endOfWeek.toInstant()
        );
    }
}
