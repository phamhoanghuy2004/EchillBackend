package com.echill.event.listener;

import com.echill.event.TestUpdatedEvent;
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
public class CacheInvalidationListener {
    CacheHelper cacheHelper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTestUpdated(TestUpdatedEvent event) {
        Long testId = event.testId();
        log.info("🎯 Nhận được tín hiệu Database đã Commit xong cho Test: {}. Bắt đầu dọn Cache!", testId);
        cacheHelper.evictTestPractice(testId);
    }
}
