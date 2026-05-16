package com.echill.helper;

import com.echill.constant.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheHelper {

    private final CacheManager cacheManager;

    public void evictCourseDetail(Long courseId) {
        if (courseId == null) return;

        Cache cache = cacheManager.getCache(CacheNames.COURSE_DETAIL);
        if (cache != null) {
            String exactKey = "course:" + courseId;
            cache.evict(exactKey);
            log.info("🧹 Đã dọn dẹp Cache khóa học ID: {}", courseId);
        }
    }

    public void evictTestPractice(Long testId) {
        if (testId == null) return;

        Cache cache = cacheManager.getCache(CacheNames.TEST_PRACTICE);
        if (cache != null) {
            cache.evict(testId);
            log.info("🔥 Đã dọn dẹp Cache bài kiểm tra ID: {}", testId);
        }
    }

    public void evictLessonDetail(Long lessonId) {
        if (lessonId == null) return;

        Cache cache = cacheManager.getCache("lessonDetails");
        if (cache != null) {
            cache.evict(lessonId);
            log.info("🧹 [CACHE CLEARED] Đã xóa cache chi tiết cho Lesson ID: {} do có cập nhật mới.", lessonId);
        }
    }

    public void evictTestPractices(List<Long> testIds) {
        Cache cache = cacheManager.getCache("testPractice");
        if (cache != null && testIds != null && !testIds.isEmpty()) {
            testIds.forEach(id -> {
                cache.evict(id);
                log.debug("🧹 Đã xóa cache testPractice cho Test ID: {}", id);
            });
            log.info("🧹 Đã xóa tổng cộng {} cache đề thi con.", testIds.size());
        }
    }
}