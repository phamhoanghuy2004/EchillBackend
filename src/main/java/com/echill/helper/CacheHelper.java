package com.echill.helper;

import com.echill.constant.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

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
}