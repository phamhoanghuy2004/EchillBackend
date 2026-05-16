package com.echill.event.listener;

import com.echill.event.TestSetUpdatedEvent;
import com.echill.repository.TestRepository;
import com.echill.helper.CacheHelper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestCacheListener {

    CacheHelper cacheHelper;
    TestRepository testRepository;

    /**
     * Chỉ dọn Cache khi Data thực sự đã nằm yên dưới DB (AFTER_COMMIT)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTestSetSyncEvent(TestSetUpdatedEvent event) {
        Long testSetId = event.testSetId();
        log.info("🎯 Nhận được tín hiệu DB Commit xong cho TestSet: {}. Bắt đầu dọn Cache các Test con!", testSetId);

        // 1. Lấy toàn bộ ID của các đề thi con thuộc bộ đề này
        List<Long> testIds = testRepository.findTestIdsByTestSetId(testSetId);

        // 2. Quét sạch Cache của chúng
        cacheHelper.evictTestPractices(testIds);
    }
}