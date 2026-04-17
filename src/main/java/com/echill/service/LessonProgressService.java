package com.echill.service;

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
    public void markVideoAsWatched(Long lessonId) {
        Long userId = SecurityUtils.getCurrentUserId();

        LessonProgress progress = progressRepository.findProgressWithLessonAndTestSet(lessonId, userId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.LESSON_NOT_STARTED));

        if (Boolean.TRUE.equals(progress.getIsVideoWatched())) {
            log.debug("Video bài {} của User {} đã được đánh dấu xem xong từ trước.", lessonId, userId);
            return;
        }

        String dataKey = PROGRESS_KEY_PREFIX + lessonId + "_" + userId;
        Object secObj = redisTemplate.opsForHash().get(dataKey, "sec");

        int currentWatchedSeconds = (secObj != null)
                ? Integer.parseInt(secObj.toString())
                : progress.getLastWatchedSecond();


        Long totalDuration = progress.getLesson().getDurationSeconds();

        if (totalDuration == null || totalDuration == 0) {
            log.debug("Lỗi thời lượng của bài học có id: {} bằng 0", lessonId);
            throw new AppException(StudentErrorEnum.LESSON_NOT_READY);
        }

        double percentage = ((double) currentWatchedSeconds / totalDuration) * 100;

        if (percentage < 90.0) {
            log.warn("🚨 [HACK ATTEMPT] Hacker detected! User {} gọi API Complete bài {} nhưng mới xem được {}%.",
                    userId, lessonId, Math.round(percentage));
            throw new AppException(StudentErrorEnum.NOT_ENOUGH_PROGRESS);
        }

        progress.markVideoWatched();

        if (progress.getLesson().getTestSet() == null) {
             Integer currentVersion = progress.getLesson().getVersion();
             progress.markAsCompleted(currentVersion);
        }


        progressRepository.save(progress);

        log.info("🎉 User {} đã xem xong video bài học {} (Tiến độ thực tế: {}%).",
                userId, lessonId, Math.round(percentage));
    }
}
