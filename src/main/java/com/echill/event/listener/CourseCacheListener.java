package com.echill.event.listener;

import com.echill.event.CourseUpdatedEvent;
import com.echill.helper.CacheHelper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseCacheListener {
    CacheHelper cacheHelper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCourseSyncEvent(CourseUpdatedEvent event) {
        Long courseId = event.courseId();
        log.info("🎯 Nhận được tín hiệu Database đã Commit xong cho Course: {}. Bắt đầu dọn Cache!", courseId);

        // Gọi Helper dọn Cache an toàn
        cacheHelper.evictCourseDetail(courseId);
    }
}