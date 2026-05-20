package com.echill.event.listener;

import com.echill.event.CourseUpdatedEvent;
import com.echill.event.LessonUpdatedEvent;
import com.echill.helper.CacheHelper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonCacheListener {
    CacheHelper cacheHelper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCourseSyncEvent(LessonUpdatedEvent event) {
        Long lessonId = event.lessonId();
        log.info("🎯 Nhận được tín hiệu Database đã Commit xong cho Lesson: {}. Bắt đầu dọn Cache!", lessonId);
        cacheHelper.evictLessonDetail(lessonId);
    }

}
