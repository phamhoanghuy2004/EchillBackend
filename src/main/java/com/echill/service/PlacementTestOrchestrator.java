package com.echill.service;

import com.echill.dto.AdaptiveTestSession;
import com.echill.dto.NextStepResponse;
import com.echill.dto.response.PlacementTestStatusResponse;
import com.echill.dto.response.QuestionPracticeResponse;
import com.echill.dto.response.AdaptiveQuestionResponse;
import com.echill.entity.enums.Level;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.StudentProfileRepository;
import com.echill.repository.TagRepository;
import com.echill.repository.TestResultRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlacementTestOrchestrator {

    AdaptiveTestingService adaptiveTestingService;
    RedisService redisService;
    SkillTrackingService skillTrackingService;

    // 🟢 DÙNG CACHE SERVICE THAY CHO QUESTION REPOSITORY
    QuestionBankCacheService questionCacheService;
    StudentService studentService;

    /**
     * API: BẮT ĐẦU BÀI TEST
     */
    public NextStepResponse startTest(Long userId) {
        PlacementTestStatusResponse status = studentService.checkPlacementTestStatus();

        if (status.isHasCompleted()) {
            log.warn("User {} đã từng làm Placement Test rồi. Chặn thi lại.", userId);
            throw new AppException(ErrorEnum.PLACEMENT_TEST_ALREADY_COMPLETED);
        }

        redisService.deleteSession(userId);

        List<Long> coreParentTagIds = questionCacheService.getCoreParentTagIdsFromCache();

        if (coreParentTagIds.isEmpty()) {
            throw new AppException(ErrorEnum.SYSTEM_PARENT_TAG_NOT_CONFIGURED);
        }

        AdaptiveTestSession session = new AdaptiveTestSession();
        session.setUserId(userId);
        session.getPendingParentTagIds().addAll(coreParentTagIds);

        log.info("🚀 User {} bắt đầu Placement Test với {} nhánh kỹ năng.", userId, coreParentTagIds.size());

        return moveToNextTagOrFinish(session);
    }

    /**
     * API: SUBMIT ĐÁP ÁN (Bọc thép 3 lớp: Lock, Idempotency, Validation)
     */
    public NextStepResponse submitAnswer(Long userId, Long questionId, Long selectedAnswerId) {
        // 🛡️ LỚP BẢO VỆ 1: CHỐNG RACE CONDITION
        if (!redisService.acquireLock(userId)) {
            log.warn("🚨 Khóa nhấp đúp bị kích hoạt cho User {}", userId);
            throw new AppException(ErrorEnum.REQUEST_PROCESSING_TOO_FAST);
        }

        try {
            AdaptiveTestSession session = redisService.getSession(userId);

            // 🛡️ LỚP BẢO VỆ 2: CHỐNG IDEMPOTENCY
            if (questionId.equals(session.getLastAnsweredQuestionId())) {
                log.warn("🚨 Kích hoạt Idempotency: User {} gửi lại câu hỏi {} đã được xử lý", userId, questionId);
                throw new AppException(ErrorEnum.QUESTION_ALREADY_PROCESSED);
            }

            // 🛡️ LỚP BẢO VỆ 3: CHỐNG HACK / TRUYỀN SAI ID
            if (!questionId.equals(session.getCurrentQuestionId())) {
                log.error("🚨 Kích hoạt Anti-Cheat: User {} nộp ID {} nhưng hệ thống đang chờ ID {}", userId, questionId, session.getCurrentQuestionId());
                throw new AppException(ErrorEnum.INVALID_QUESTION_ID);
            }

            // --- LOGIC XỬ LÝ CHÍNH ---
            QuestionPracticeResponse practiceQ = questionCacheService.getCachedQuestionById(questionId);
            if (practiceQ == null) {
                throw new AppException(ErrorEnum.QUESTION_NOT_FOUND_OR_CACHE_EXPIRED);
            }

            boolean isCorrect = false;
            if (selectedAnswerId != null) {
                isCorrect = practiceQ.getAnswers().stream()
                        .anyMatch(a -> a.getId().equals(selectedAnswerId) && Boolean.TRUE.equals(a.getIsCorrect()));
            }

            // 🟢 FIX: Lấy 1 Tag ID duy nhất truyền xuống Lõi CAT
            Long childTagId = practiceQ.getChildTagId();
            boolean shouldContinueCurrentTag = adaptiveTestingService.processAnswer(
                    session, questionId, isCorrect, childTagId
            );

            // Cập nhật State Idempotency & dọn dẹp Current Question
            session.setLastAnsweredQuestionId(questionId);
            session.setCurrentQuestionId(null);

            if (shouldContinueCurrentTag) {
                AdaptiveQuestionResponse nextQ = adaptiveTestingService.getNextQuestion(session);

                if (nextQ == null) {
                    log.warn("⚠️ Kho Data cạn kiệt cho nhánh {}. Ép chốt điểm và chuyển nhánh.", session.getCurrentParentTagId());
                    skillTrackingService.processPlacementTestFinish(
                            session.getUserId(), session.getCurrentParentTagId(),
                            session.getHighestPassedLevel(), session.getTestedChildTags()
                    );
                    return moveToNextTagOrFinish(session);
                }

                redisService.saveSession(userId, session);
                return new NextStepResponse(false, nextQ, "Tiếp tục nhánh hiện tại");
            } else {
                return moveToNextTagOrFinish(session);
            }
        } finally {
            // 🛡️ Bắt buộc phải nhả Lock
            redisService.releaseLock(userId);
        }
    }

    /**
     * LOGIC ĐIỀU HƯỚNG CỐT LÕI (ROUTER)
     */
    private NextStepResponse moveToNextTagOrFinish(AdaptiveTestSession session) {
        if (session.getPendingParentTagIds().isEmpty()) {
            log.info("🎉 User {} đã hoàn thành toàn bộ Placement Test!", session.getUserId());

            studentService.updateOverallStudentLevel(session.getUserId(), true);
            log.info("📈 Đã tính toán và cập nhật Level tổng thành công cho User {}", session.getUserId());

            redisService.deleteSession(session.getUserId());
            return new NextStepResponse(true, null, "Hoàn thành bài thi!");
        }

        Long nextParentTagId = session.getPendingParentTagIds().poll();
        session.resetForNewParentTag(nextParentTagId);

        log.info("🔄 Chuyển User {} sang nhánh Tag Cha mới: ID {}", session.getUserId(), nextParentTagId);

        // Bốc DTO an toàn cho nhánh mới
        AdaptiveQuestionResponse firstQuestionOfNewTag = adaptiveTestingService.getNextQuestion(session);

        if (firstQuestionOfNewTag == null) {
            log.warn("⚠️ BỎ QUA NHÁNH: Tag Cha ID {} không có dữ liệu câu hỏi khởi động (Level 3).", nextParentTagId);
            return moveToNextTagOrFinish(session);
        }

        redisService.saveSession(session.getUserId(), session);

        return new NextStepResponse(false, firstQuestionOfNewTag, "Đã chuyển sang nhánh kỹ năng mới");
    }
}