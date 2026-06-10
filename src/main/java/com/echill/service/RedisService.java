package com.echill.service;

import com.echill.dto.AdaptiveTestSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Định dạng Key chuẩn: [Tên dự án/Module]:[Tên thực thể]:[ID]
    private static final String SESSION_KEY_PREFIX = "echill:adaptive_test:session:";
    private static final long SESSION_TTL_HOURS = 2; // Tự động dọn rác sau 2 giờ

    private static final String LOCK_KEY_PREFIX = "echill:lock:adaptive_test:";
    private static final long LOCK_TIMEOUT_SECONDS = 5; // Timeout bảo vệ tránh Deadlock

    public void saveSession(Long userId, AdaptiveTestSession session) {
        String key = SESSION_KEY_PREFIX + userId;
        try {
            // Chuyển Java Object thành chuỗi JSON
            String jsonValue = objectMapper.writeValueAsString(session);
            // Lưu vào Redis kèm thời gian hết hạn
            redisTemplate.opsForValue().set(key, jsonValue, Duration.ofHours(SESSION_TTL_HOURS));
        } catch (JsonProcessingException e) {
            log.error("❌ Lỗi parse JSON khi lưu Session cho User {}: {}", userId, e.getMessage());
            throw new RuntimeException("Hệ thống lỗi: Không thể lưu trạng thái bài thi.");
        }
    }

    public AdaptiveTestSession getSession(Long userId) {
        String key = SESSION_KEY_PREFIX + userId;
        String jsonValue = redisTemplate.opsForValue().get(key);

        if (jsonValue == null) {
            log.warn("⚠️ Không tìm thấy Session cho User {} (Có thể đã hết hạn TTL)", userId);
            throw new RuntimeException("Phiên làm bài đã hết hạn hoặc không tồn tại. Vui lòng làm lại từ đầu.");
        }

        try {
            // Dịch ngược chuỗi JSON thành Java Object
            return objectMapper.readValue(jsonValue, AdaptiveTestSession.class);
        } catch (JsonProcessingException e) {
            log.error("❌ Lỗi parse JSON khi đọc Session của User {}: {}", userId, e.getMessage());
            throw new RuntimeException("Hệ thống lỗi: Không thể đọc dữ liệu phiên thi.");
        }
    }

    public void deleteSession(Long userId) {
        String key = SESSION_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("🗑️ Đã dọn dẹp Session bài thi của User {} trên Redis", userId);
    }

    /**
     * 🔥 VÁ LỖ HỔNG 1: Xin khóa (Acquire Lock) cho User
     * Dùng setIfAbsent (tương đương SETNX trong Redis).
     * Nếu key chưa tồn tại -> Gán thành công (trả về true).
     * Nếu key đã có (luồng khác đang xử lý) -> Trả về false.
     */
    public boolean acquireLock(Long userId) {
        String lockKey = LOCK_KEY_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(LOCK_TIMEOUT_SECONDS)));
    }

    /**
     * Giải phóng khóa (Release Lock)
     */
    public void releaseLock(Long userId) {
        String lockKey = LOCK_KEY_PREFIX + userId;
        redisTemplate.delete(lockKey);
    }

    /**
     * Xóa cache theo pattern
     */
    public void clearCacheByPattern(String pattern) {
        java.util.Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("🗑️ Đã xóa {} keys với pattern: {}", keys.size(), pattern);
        }
    }

    /**
     * Xóa toàn bộ system caches theo yêu cầu
     */
    public void clearAllSystemCaches() {
        String[] cacheNames = {
            "courseDetail", "categories", "testPractice", "testReviewCache", 
            "topStudents", "lessonDetails", "tags", "allCourses", 
            "allTeachers", "latestBlogs", "testSetDetails", "testQuestionCounts", 
            "testSectionSummaries", "allReviews", "getMyReviewByCourse", 
            "allReviewsByCourse", "featuredReviews"
        };
        for (String cacheName : cacheNames) {
            clearCacheByPattern(cacheName + "::*"); // Xóa tất cả các keys trong từng namespace của Spring Cache
        }
        
        // Xóa cache session placement test và lock
        clearCacheByPattern(SESSION_KEY_PREFIX + "*");
        clearCacheByPattern(LOCK_KEY_PREFIX + "*");
        
        log.info("✅ Đã xóa toàn bộ cache hệ thống (System Caches + Placement Test Sessions).");
    }
}