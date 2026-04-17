package com.echill.job;

import com.echill.repository.LessonProgressBatchRepository;
import com.echill.repository.LessonProgressBatchRepository.ProgressBatchItem;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProgressSyncJob {

    StringRedisTemplate redisTemplate;
    LessonProgressBatchRepository batchRepository;

    static String DIRTY_ZSET_KEY = "progress:dirty:v2";
    static String SYNC_LOCK_KEY = "lock:progress-sync";

    @Scheduled(fixedDelay = 300000)
    public void syncProgressFromRedisToMySQL() {
        Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(SYNC_LOCK_KEY, "running", Duration.ofMinutes(4));
        if (Boolean.FALSE.equals(isLocked)) {
            log.debug("Job đồng bộ đang được chạy ở Server/Pod khác. Bỏ qua...");
            return;
        }

        try {
            Long redisTimeMs = Objects.requireNonNull(
                    redisTemplate.getConnectionFactory(),
                    "Redis CF must not be null"
            ).getConnection().serverCommands().time();
            long syncStartTimeSec = (redisTimeMs != null ? redisTimeMs : System.currentTimeMillis()) / 1000;

            int batchSize = 1000;
            int totalProcessed = 0;

            while (true) {
                Set<String> dirtyKeys = redisTemplate.opsForZSet().rangeByScore(
                        DIRTY_ZSET_KEY, 0, syncStartTimeSec, 0, batchSize);

                if (dirtyKeys == null || dirtyKeys.isEmpty()) {
                    break;
                }

                List<String> keysArray = new ArrayList<>(dirtyKeys);

                List<Object> pipelineResults = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (String key : keysArray) {
                        connection.hashCommands().hGet(key.getBytes(), "sec".getBytes());
                    }
                    return null;
                });

                // Map thô -> DTO
                List<ProgressBatchItem> batchItems = new ArrayList<>();
                for (int i = 0; i < keysArray.size(); i++) {
                    String key = keysArray.get(i);
                    Object valueObj = pipelineResults.get(i);

                    if (valueObj != null) {
                        String[] parts = key.split(":")[3].split("_");
                        Long lessonId = Long.parseLong(parts[0]);
                        Long userId = Long.parseLong(parts[1]);
                        Integer currentSecond = Integer.parseInt((String) valueObj);
                        batchItems.add(new ProgressBatchItem(lessonId, userId, currentSecond));
                    }
                }

                if (!batchItems.isEmpty()) {
                    batchRepository.batchUpdateProgress(batchItems);
                }

                redisTemplate.opsForZSet().remove(DIRTY_ZSET_KEY, dirtyKeys.toArray());

                totalProcessed += batchItems.size();
            }

            if (totalProcessed > 0) {
                log.info("✅ [CronJob] Đồng bộ thành công tổng cộng {} records (Chia lô {} records/lần).", totalProcessed, batchSize);
            }

        } catch (Exception e) {
            log.error("❌ [CronJob] Lỗi đồng bộ tiến độ.", e);
        } finally {
            redisTemplate.delete(SYNC_LOCK_KEY);
        }
    }
}