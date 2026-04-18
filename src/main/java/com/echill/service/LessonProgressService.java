package com.echill.service;

import com.echill.dto.response.learner.VideoCompleteResponse;
import com.echill.entity.LessonProgress;
import com.echill.exception.AppException;
import com.echill.exception.StudentErrorEnum;
import com.echill.repository.LessonProgressRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonProgressService {
    LessonProgressRepository progressRepository;
    StringRedisTemplate redisTemplate;

    static String PROGRESS_KEY_PREFIX = "progress:data:v2:";

    @Transactional
    public VideoCompleteResponse markVideoAsWatched(Long lessonId) {
        Long userId = SecurityUtils.getCurrentUserId();

        LessonProgress progress = progressRepository.findProgressWithLessonAndTestSet(lessonId, userId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.LESSON_NOT_STARTED));

        if (Boolean.TRUE.equals(progress.getIsVideoWatched())) {
            log.debug("♻️ Video bài {} của User {} đã được đánh dấu xem xong từ trước.", lessonId, userId);
            return VideoCompleteResponse.builder()
                    .videoWatched(true)
                    .lessonCompleted(Boolean.TRUE.equals(progress.getIsCompleted()))
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
        boolean quizAlreadyPassed = Boolean.TRUE.equals(progress.getIsQuizPassed());

        if (!hasTest || quizAlreadyPassed) {
            Integer currentVersion = progress.getLesson().getVersion();
            progress.markAsCompleted(currentVersion);
            log.info("🏆 [LESSON COMPLETED] User {} đã hoàn thành trọn vẹn bài học {} (Không có bài tập hoặc đã Pass).",
                    userId, lessonId);
        } else {
            log.info("⏳ [WAITING QUIZ] User {} đã xong video nhưng cần làm bài tập để hoàn thành bài học {}.",
                    userId, lessonId);
        }

        progressRepository.save(progress);

        return VideoCompleteResponse.builder()
                .videoWatched(true)
                .lessonCompleted(Boolean.TRUE.equals(progress.getIsCompleted()))
                .build();
    }
}
