package com.echill.service.redis;

import com.echill.dto.response.learner.ProgressStatusResponse;
import com.echill.entity.LessonProgress;
import com.echill.exception.AppException;
import com.echill.exception.StudentErrorEnum; // Đảm bảo bạn có Enum lỗi phù hợp
import com.echill.repository.LessonProgressRepository;
import com.echill.util.SecurityUtils;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProgressRedisService {

    StringRedisTemplate redisTemplate;
    LessonProgressRepository progressRepository;

    // ==========================================
    // CONSTANTS & CONFIGURATIONS
    // ==========================================
    static final String DIRTY_ZSET_KEY = "progress:dirty:v2";
    static final String PROGRESS_KEY_PREFIX = "progress:data:v2:";

    static final long CACHE_TTL_SECONDS = 86400L; // 1 Ngày
    static final int NETWORK_BUFFER_SECONDS = 10; // Sai số lag mạng
    static final double MAX_SYSTEM_SPEED = 2.5;   // Giới hạn tốc độ trần (Clamp Limit)

    @NonFinal
    DefaultRedisScript<Long> updateProgressScript;

    @PostConstruct
    public void init() {
        // 💥 LUA SCRIPT V3: ANTI-CHEAT TUYỆT ĐỐI
        String lua = """
            local dataKey = KEYS[1]
            local dirtyKey = KEYS[2]
            local newSec = tonumber(ARGV[1])
            local currentTs = tonumber(ARGV[2])
            local ttl = tonumber(ARGV[3])
            local maxSpeed = tonumber(ARGV[4])
            local buffer = tonumber(ARGV[5])
            
            local oldSec = tonumber(redis.call('HGET', dataKey, 'sec'))
            local oldTs = tonumber(redis.call('HGET', dataKey, 'ts'))
            
            if oldSec and oldTs then
                -- Nếu tua lùi hoặc xem lại đoạn cũ -> Bỏ qua không cộng thêm
                if newSec <= oldSec then return 0 end
            
                local timeElapsed = currentTs - oldTs
                if timeElapsed < 0 then timeElapsed = 0 end
                
                -- 🛡️ CHỐT CHẶN 1: KHÓA TRẦN (Chống Pause để tích lũy thời gian)
                if timeElapsed > 40 then 
                    timeElapsed = 40 
                end
            
                local maxAllowedJump = (timeElapsed * maxSpeed) + buffer
                if (newSec - oldSec) > maxAllowedJump then
                    -- 🛡️ CHỐT CHẶN 2: RÚT CẠN THỜI GIAN (Hình phạt Tua láo)
                    redis.call('HSET', dataKey, 'ts', currentTs)
                    redis.call('EXPIRE', dataKey, ttl)
                    return -1
                end
            end
            
            -- Hợp lệ: Cập nhật Tiến độ & Timestamp mới
            redis.call('HSET', dataKey, 'sec', newSec, 'ts', currentTs)
            redis.call('EXPIRE', dataKey, ttl)
            redis.call('ZADD', dirtyKey, currentTs, dataKey)
            return 1
            """;
        updateProgressScript = new DefaultRedisScript<>(lua, Long.class);
    }

    /**
     * Ghi nhận nhịp tim tiến độ từ Frontend
     * @param lessonId ID bài học
     * @param currentSecond Số giây hiện tại
     * @param feSpeed Tốc độ đang xem do FE gửi lên (Ví dụ: 1.0, 1.5, 2.0)
     */
    public void recordHeartbeat(Long lessonId, Integer currentSecond, Double feSpeed) {
        Long userId = SecurityUtils.getCurrentUserId();
        String dataKey = PROGRESS_KEY_PREFIX + lessonId + "_" + userId;

        try {
            long currentTimestampSec = getCurrentRedisTimeSeconds();
            ensureCacheExists(dataKey, lessonId, userId, currentTimestampSec);

            // 🛡️ BỘ LỌC DỐI TRÁ (CLAMP LIMIT)
            double actualSpeed = (feSpeed != null && feSpeed > 0) ? feSpeed : 1.0;
            double safeSpeed = Math.min(actualSpeed, MAX_SYSTEM_SPEED);

            List<String> keys = List.of(dataKey, DIRTY_ZSET_KEY);
            Long result = redisTemplate.execute(
                    updateProgressScript,
                    keys,
                    String.valueOf(currentSecond),
                    String.valueOf(currentTimestampSec),
                    String.valueOf(CACHE_TTL_SECONDS),
                    String.valueOf(safeSpeed),
                    String.valueOf(NETWORK_BUFFER_SECONDS)
            );

            // Xử lý kẻ gian lận
            if (Long.valueOf(-1L).equals(result)) {
                log.warn("🚨 [HACK DETECTED] User {} tua quá nhanh bài {} (Speed Filter: {}x)",
                        userId, lessonId, safeSpeed);
                // Ném lỗi 400 để Frontend bắt và giật lùi Video
                throw new AppException(StudentErrorEnum.NOT_ENOUGH_PROGRESS);
            }

        } catch (AppException e) {
            throw e; // Quăng tiếp cho GlobalExceptionHandler xử lý
        } catch (Exception e) {
            log.error("🔥 Redis Error! Falling back to DB safe update...", e);
            fallbackSaveToDatabase(lessonId, userId, currentSecond);
        }
    }

    /**
     * Lấy tiến độ hiện tại (Dùng khi F5 hoặc mở bài học)
     */
    @Transactional
    public ProgressStatusResponse getCurrentProgress(Long lessonId) {
        Long userId = SecurityUtils.getCurrentUserId();
        String dataKey = PROGRESS_KEY_PREFIX + lessonId + "_" + userId;

        LessonProgress progress = progressRepository.findProgressWithLesson(lessonId, userId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.LESSON_NOT_STARTED));

        Integer currentVersion = progress.getLesson().getVersion();

        boolean isNewVersion = currentVersion > progress.getLastSeenVersion();

        boolean isOutdated = Boolean.TRUE.equals(progress.getIsCompleted())
                && !currentVersion.equals(progress.getVersionCompleted());

        if (isNewVersion) {
            progress.setLastSeenVersion(currentVersion);
            progress.setLastWatchedSecond(0);
            log.info("🔄 Lazy Update: Đã đồng bộ Version {} cho User {} tại Bài {}", currentVersion, userId, lessonId);
        }

        int currentSecond = progress.getLastWatchedSecond();

        Object secObj = !isNewVersion ? redisTemplate.opsForHash().get(dataKey, "sec") : null;

        if (secObj != null) {
            currentSecond = Integer.parseInt(secObj.toString());
        } else {
            try {
                redisTemplate.opsForHash().put(dataKey, "sec", String.valueOf(currentSecond));
                redisTemplate.opsForHash().put(dataKey, "ts", String.valueOf(getCurrentRedisTimeSeconds()));
                redisTemplate.expire(dataKey, Duration.ofSeconds(CACHE_TTL_SECONDS));
            } catch (Exception e) {
                log.warn("⚠️ Sync Cache thất bại: {}", e.getMessage());
            }
        }

        return ProgressStatusResponse.builder()
                .currentSecond(currentSecond)
                .isVideoWatched(Boolean.TRUE.equals(progress.getIsVideoWatched()) && !isOutdated)
                .isQuizPassed(Boolean.TRUE.equals(progress.getIsQuizPassed()) && !isOutdated)
                .build();
    }

    // ==========================================
    // HELPER METHODS (DRY)
    // ==========================================

    private long getCurrentRedisTimeSeconds() {
        try {
            Long redisTimeMs = Objects.requireNonNull(
                    redisTemplate.getConnectionFactory(),
                    "Redis ConnectionFactory must not be null"
            ).getConnection().serverCommands().time();

            return (redisTimeMs != null ? redisTimeMs : System.currentTimeMillis()) / 1000;
        } catch (Exception e) {
            log.warn("Không lấy được giờ Redis, Fallback về System Time");
            return System.currentTimeMillis() / 1000;
        }
    }

    private void ensureCacheExists(String dataKey, Long lessonId, Long userId, long currentTimestampSec) {
        if (redisTemplate.hasKey(dataKey)) {
            return;
        }

        progressRepository.findByLessonIdAndEnrollmentStudentId(lessonId, userId)
                .ifPresent(p -> {
                    redisTemplate.opsForHash().putIfAbsent(dataKey, "sec", String.valueOf(p.getLastWatchedSecond()));
                    redisTemplate.opsForHash().putIfAbsent(dataKey, "ts", String.valueOf(currentTimestampSec));
                    redisTemplate.expire(dataKey, Duration.ofSeconds(CACHE_TTL_SECONDS));
                });
    }

    private void fallbackSaveToDatabase(Long lessonId, Long userId, Integer currentSecond) {
        try {
            progressRepository.updateAtomicProgress(lessonId, userId, currentSecond);
        } catch (Exception ex) {
            log.error("💥 Critical: Failed to fallback update DB for user {}", userId);
        }
    }
}