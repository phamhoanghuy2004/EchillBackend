package com.echill.service;

import com.echill.dto.response.AnswerPracticeResponse;
import com.echill.dto.response.QuestionPracticeResponse;
import com.echill.dto.response.AdaptiveAnswerResponse;
import com.echill.dto.response.AdaptiveQuestionResponse;
import com.echill.entity.Question;
import com.echill.entity.Tag;
import com.echill.repository.QuestionRepository;
import com.echill.repository.TagRepository;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionBankCacheService {

    QuestionRepository questionRepository;
    TagRepository tagRepository;

    // =====================================================================
    // 🧠 L1 CACHE MEMORY (RAM)
    // =====================================================================

    // Map 1: Phục vụ bốc random câu hỏi. Key: "parentTagId_level" -> List<QuestionId>
    @NonFinal
    private volatile Map<String, List<Long>> questionPools = new ConcurrentHashMap<>();

    // Map 2: Phục vụ chấm điểm O(1). Key: QuestionId -> QuestionPracticeResponse (Có đáp án)
    @NonFinal
    private volatile Map<Long, QuestionPracticeResponse> questionLookup = new ConcurrentHashMap<>();

    java.util.concurrent.atomic.AtomicReference<List<Long>> coreParentTagIdsCache = new java.util.concurrent.atomic.AtomicReference<>(List.of());

    /**
     * Tự động chạy khi Server khởi động để nạp dữ liệu từ MySQL lên RAM
     */
    @PostConstruct
    public void loadPlacementQuestionsToCache() {
        log.info("⏳ Đang nạp Ngân hàng câu hỏi & Cấu trúc Tag Placement Test vào RAM...");

        // 1. NẠP TAG CHA VÀO RAM
        List<Long> rootTags = tagRepository.findCoreParentTagIds();
        if (rootTags.isEmpty()) {
            log.error("❌ CẢNH BÁO CRITICAL: Hệ thống chưa có Tag Cha nào được cấu hình!");
        } else {
            // Lưu thành list chỉ đọc (Thread-safe)
            coreParentTagIdsCache.set(List.copyOf(rootTags));
            log.info("✅ Đã nạp cấu trúc nhánh kỹ năng. Có {} nhánh cốt lõi.", rootTags.size());
        }

        // 2. NẠP CÂU HỎI VÀO RAM
        List<Question> questions = questionRepository.findAllPlacementTestQuestions();

        if (questions.isEmpty()) {
            log.warn("⚠️ CẢNH BÁO: Kho dữ liệu Placement Test đang trống!");
            return;
        }

        // 🔥 FIX BUG #1: TẠO MAP TẠM THỜI (DOUBLE BUFFERING)
        Map<String, List<Long>> tempQuestionPools = new ConcurrentHashMap<>();
        Map<Long, QuestionPracticeResponse> tempQuestionLookup = new ConcurrentHashMap<>();

        for (Question q : questions) {

            // 🟢 TỐI ƯU MỚI: Xử lý 1 Tag duy nhất
            Long childTagId = null;
            Long parentTagId = null;

            if (q.getTag() != null) {
                if (q.getTag().getParent() != null) {
                    // Nếu admin gán chuẩn (gán Tag Con)
                    childTagId = q.getTag().getId();
                    parentTagId = q.getTag().getParent().getId();
                } else {
                    // Nếu admin lỡ tay gán trực tiếp Tag Cha vào câu hỏi
                    parentTagId = q.getTag().getId();
                }
            }

            // Map Entity -> DTO Thực hành
            List<AnswerPracticeResponse> practiceAnswers = q.getAnswers().stream()
                    .map(a -> AnswerPracticeResponse.builder()
                            .id(a.getId())
                            .content(a.getContent())
                            .isCorrect(a.getIsCorrect())
                            .build())
                    .toList();

            QuestionPracticeResponse practiceQ = QuestionPracticeResponse.builder()
                    .id(q.getId())
                    .content(q.getContent())
                    .audioUrl(q.getAudioUrl())
                    .imageUrl(q.getImageUrl())
                    .childTagId(childTagId) // Truyền đúng 1 ID
                    .answers(practiceAnswers)
                    .build();

            // 👉 Nạp vào Map TẠM
            tempQuestionLookup.put(q.getId(), practiceQ);

            // 👉 Phân loại vào Map TẠM (Chỉ nhét vào 1 ngăn kéo duy nhất)
            if (parentTagId != null) {
                String poolKey = parentTagId + "_" + q.getDifficultyLevel();
                tempQuestionPools.computeIfAbsent(poolKey, k -> new java.util.ArrayList<>()).add(q.getId());
            } else {
                log.warn("⚠️ Câu hỏi ID {} không có Tag hoặc bị lỗi cấu trúc Tag, sẽ bị bỏ qua trong luồng CAT!", q.getId());
            }
        }

        // 🔥 FIX BUG #1: TRÁO CON TRỎ (REFERENCE SWAP) & ĐÓNG BĂNG LIST
        tempQuestionPools.replaceAll((key, list) -> Collections.unmodifiableList(list));
        this.questionPools = tempQuestionPools;
        this.questionLookup = tempQuestionLookup;

        log.info("✅ Đã nạp thành công {} câu hỏi Placement Test vào L1 Cache!", questions.size());
    }

    // ======================================================
    // CÁC HÀM GIAO TIẾP VỚI BÊN NGOÀI (Lấy dữ liệu từ Cache)
    // ======================================================

    public List<Long> getCoreParentTagIdsFromCache() {
        return coreParentTagIdsCache.get();
    }

    public List<Long> getQuestionIdsForPool(Long parentTagId, Integer level) {
        return questionPools.getOrDefault(parentTagId + "_" + level, List.of());
    }

    public QuestionPracticeResponse getCachedQuestionById(Long questionId) {
        return questionLookup.get(questionId);
    }

    public AdaptiveQuestionResponse convertToSafeResponse(QuestionPracticeResponse practiceQ) {
        List<AdaptiveAnswerResponse> safeAnswers = practiceQ.getAnswers().stream()
                .map(a -> AdaptiveAnswerResponse.builder()
                        .id(a.getId())
                        .content(a.getContent())
                        .build())
                .toList();

        return AdaptiveQuestionResponse.builder()
                .id(practiceQ.getId())
                .content(practiceQ.getContent())
                .audioUrl(practiceQ.getAudioUrl())
                .imageUrl(practiceQ.getImageUrl())
                .answers(safeAnswers)
                .build();
    }
}