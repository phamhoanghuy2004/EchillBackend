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
    com.echill.repository.TestRepository testRepository;
    com.echill.service.QuestionBankCacheService questionBankCacheService;

    @org.springframework.scheduling.annotation.Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTestUpdated(TestUpdatedEvent event) {
        Long testId = event.testId();
        log.info("🎯 Nhận được tín hiệu Database đã Commit xong cho Test: {}. Bắt đầu dọn Cache!", testId);
        
        // 1. Dọn Redis Cache cho bài thi thường
        cacheHelper.evictTestPractice(testId);
        
        // 2. Cập nhật lại RAM Cache nếu đây là Placement Test
        testRepository.findById(testId).ifPresent(test -> {
            if (test.getType() == com.echill.entity.enums.TestType.PLACEMENT_TEST) {
                log.info("🔄 Test {} là Placement Test. Đang tải lại bộ nhớ đệm RAM...", testId);
                questionBankCacheService.loadPlacementQuestionsToCache();
            }
        });
    }
}
