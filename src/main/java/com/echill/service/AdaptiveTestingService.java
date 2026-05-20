package com.echill.service;

import com.echill.dto.AdaptiveTestSession;
import com.echill.dto.response.QuestionPracticeResponse;
import com.echill.dto.response.AdaptiveQuestionResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdaptiveTestingService {

    // 🟢 Đã gỡ bỏ QuestionRepository, 100% xài Cache
    QuestionBankCacheService questionCacheService;
    SkillTrackingService skillTrackingService;

    // =====================================================================
    // ⚙️ SYSTEM CONSTANTS (CẤU HÌNH THUẬT TOÁN)
    // =====================================================================
    private static final int MAX_QUESTIONS_PER_TAG = 5;
    private static final int MAX_LEVEL = 5;
    private static final int MIN_LEVEL = 1;
    private static final int MAX_CONSECUTIVE_FAILS_AT_BOTTOM = 2;

    /**
     * BƯỚC 1: Lấy câu hỏi tiếp theo (TỪ RAM CACHE O(1))
     */
    public AdaptiveQuestionResponse getNextQuestion(AdaptiveTestSession session) {
        // 1. Lấy rổ ID câu hỏi từ Pool
        List<Long> poolIds = questionCacheService.getQuestionIdsForPool(
                session.getCurrentParentTagId(),
                session.getCurrentLevel()
        );

        // 2. Lọc bỏ các ID đã làm (Xử lý trên RAM siêu nhanh)
        Set<Long> askedIds = session.getAskedQuestionIds();
        List<Long> eligibleIds = poolIds.stream()
                .filter(id -> !askedIds.contains(id))
                .toList();

        if (eligibleIds.isEmpty()) {
            log.warn("Hết câu hỏi ở Level {} cho Tag {}.", session.getCurrentLevel(), session.getCurrentParentTagId());
            return null;
        }

        // 3. Randomize và bốc câu đầu tiên
        int randomIndex = ThreadLocalRandom.current().nextInt(eligibleIds.size());
        Long selectedQuestionId = eligibleIds.get(randomIndex);

        session.addAskedQuestion(selectedQuestionId);

        session.setCurrentQuestionId(selectedQuestionId);

        // 4. Lấy DTO từ Cache và "Rửa" qua hàm an toàn trước khi trả về
        QuestionPracticeResponse practiceQ = questionCacheService.getCachedQuestionById(selectedQuestionId);
        return questionCacheService.convertToSafeResponse(practiceQ);
    }

    /**
     * BƯỚC 2: Xử lý sau khi User Submit đáp án
     */
    public boolean processAnswer(AdaptiveTestSession session, Long questionId, boolean isCorrect, Long childTagId) {
        log.info("User {} trả lời câu {} (Level {}) - Kết quả: {}",
                session.getUserId(), questionId, session.getCurrentLevel(), isCorrect);

        boolean shouldContinue = isCorrect
                ? handleCorrectAnswer(session, childTagId)
                : handleIncorrectAnswer(session);

        if (!shouldContinue) return false;

        return checkExitConditions(session);
    }

    // =====================================================================
    // 🛠️ PRIVATE HELPER METHODS
    // =====================================================================

    private boolean handleCorrectAnswer(AdaptiveTestSession session, Long childTagId) {
        session.setHighestPassedLevel(Math.max(session.getHighestPassedLevel(), session.getCurrentLevel()));

        // 🟢 Đã đổi từ xử lý List sang xử lý 1 Tag duy nhất
        if (childTagId != null) {
            session.getTestedChildTags().put(childTagId, session.getCurrentLevel());
        }

        if (session.getCurrentLevel() == MAX_LEVEL) {
            log.info("🏆 MASTER DETECTED! User {} phá đảo Level {} nhánh {}. Đóng nhánh.",
                    session.getUserId(), MAX_LEVEL, session.getCurrentParentTagId());
            finalizeCurrentParentTag(session);
            return false;
        }

        session.setCurrentLevel(session.getCurrentLevel() + 1);
        session.setConsecutiveFailsAtLevel1(0);
        return true;
    }

    private boolean handleIncorrectAnswer(AdaptiveTestSession session) {
        if (session.getCurrentLevel() == MIN_LEVEL) {
            session.setConsecutiveFailsAtLevel1(session.getConsecutiveFailsAtLevel1() + 1);
            return true;
        }

        int nextLevel = session.getCurrentLevel() - 1;

        if (nextLevel <= session.getHighestPassedLevel()) {
            log.info("🔥 CEILING DETECTED! Trần năng lực User {} là Level {}. Đóng nhánh.",
                    session.getUserId(), session.getHighestPassedLevel());
            finalizeCurrentParentTag(session);
            return false;
        }

        session.setCurrentLevel(nextLevel);
        return true;
    }

    private boolean checkExitConditions(AdaptiveTestSession session) {
        if (session.getQuestionsAskedCount() >= MAX_QUESTIONS_PER_TAG) {
            log.info("User {} đã làm đủ {} câu nhánh {}. Chuyển nhánh.",
                    session.getUserId(), MAX_QUESTIONS_PER_TAG, session.getCurrentParentTagId());
            finalizeCurrentParentTag(session);
            return false;
        }

        if (session.getConsecutiveFailsAtLevel1() >= MAX_CONSECUTIVE_FAILS_AT_BOTTOM) {
            log.info("🔴 FLOOR DETECTED! User {} mất gốc nhánh {}. Đóng nhánh.",
                    session.getUserId(), session.getCurrentParentTagId());
            finalizeCurrentParentTag(session);
            return false;
        }

        return true;
    }

    private void finalizeCurrentParentTag(AdaptiveTestSession session) {
        skillTrackingService.processPlacementTestFinish(
                session.getUserId(),
                session.getCurrentParentTagId(),
                session.getHighestPassedLevel(),
                session.getTestedChildTags()
        );
    }
}