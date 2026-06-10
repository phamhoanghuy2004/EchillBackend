package com.echill.service;

import com.echill.dto.response.ImportTestResponse;
import com.echill.dto.toeic.ToeicExcelRowDto;
import com.echill.entity.*;
import com.echill.entity.enums.TagGroup;
import com.echill.entity.enums.TestType;
import com.echill.exception.AppException;
import com.echill.exception.TeacherErrorEnum;
import com.echill.repository.QuestionRepository;
import com.echill.repository.TagRepository;
import com.echill.repository.TestRepository;
import com.echill.repository.TestSetRepository;
import com.echill.util.SecurityUtils;
import com.echill.util.ToeicExcelParser;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * Handles importing a full TOEIC test from Excel.
 *
 * Flow:
 *  1. Parse Excel → List<ToeicExcelRowDto>
 *  2. Validate each row
 *  3. Build Test + 7 TestSections (Part 1–7)
 *  4. Group rows by groupCode → QuestionGroup (Part 3,4,6,7)
 *  5. Standalone rows (null groupCode) → directly into section.questions (Part 1,2,5)
 *  6. Save atomically
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImportTestService {

    ToeicExcelParser toeicExcelParser;
    TestRepository testRepository;
    TestSetRepository testSetRepository;
    QuestionRepository questionRepository;
    TagRepository tagRepository;
    QuestionBankCacheService questionBankCacheService;
    org.springframework.context.ApplicationEventPublisher eventPublisher;

    /** Cloudinary folder structure */
    private static final String FOLDER_QUESTION_AUDIO = "toeic/audio/questions";
    private static final String FOLDER_QUESTION_IMAGE = "toeic/image/questions";
    private static final String FOLDER_GROUP_AUDIO    = "toeic/audio/groups";
    private static final String FOLDER_GROUP_IMAGE    = "toeic/image/groups";

    private static final String EXCEL_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    // -------------------------------------------------------------------
    // PUBLIC API
    // -------------------------------------------------------------------

    /**
     * Parse and persist a TOEIC test from an Excel file into the given TestSet.
     * The testSetId is passed from the controller (admin picks the TestSet first).
     */
    @Transactional(rollbackFor = Exception.class)
    public ImportTestResponse importFromExcel(Long testSetId, String testTitle, MultipartFile file) {
        log.info("Starting import – TestSet: {}, Title: {}", testSetId, testTitle);

        validateFile(file);

        List<ToeicExcelRowDto> rows = toeicExcelParser.parse(file);
        if (rows.isEmpty()) {
            throw new AppException(TeacherErrorEnum.FILE_EMPTY);
        }

        TestSet testSet = testSetRepository.findById(testSetId)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.TEST_SET_NOT_FOUND));

        boolean isPlacementTest = testSet.getType() == TestType.PLACEMENT_TEST;

        // Build in-memory object graph -----------------------------------
        Test test = Test.builder()
                .title(testTitle)
                .durationMinutes(isPlacementTest ? 30 : 120)
                .passScore(0.0)
                .testSet(testSet)
                .type(testSet.getType())
                .build();

        // Create sections: 1 flat section for PLACEMENT_TEST, 7 parts for TOEIC
        Map<Integer, TestSection> sectionByPart = new LinkedHashMap<>();
        if (isPlacementTest) {
            TestSection section = TestSection.builder()
                    .title("Câu hỏi")
                    .orderIndex(1)
                    .build();
            test.addSection(section);
            sectionByPart.put(1, section);
        } else {
            for (int part = 1; part <= 7; part++) {
                TestSection section = TestSection.builder()
                        .title("Part " + part)
                        .orderIndex(part)
                        .build();
                test.addSection(section);
                sectionByPart.put(part, section);
            }
        }

        // Map<groupCode, QuestionGroup> – one group per unique groupCode per part
        Map<String, QuestionGroup> groupMap = new LinkedHashMap<>();

        // Map<tagName, Tag> – Cache for tags during this import session
        Map<String, Tag> tagCache = new HashMap<>();

        int totalQuestions = 0;
        int totalGroups    = 0;

        for (int i = 0; i < rows.size(); i++) {
            ToeicExcelRowDto row = rows.get(i);
            int rowIndex = i + 1; // 1-based index for logging

            try {
                validateRow(row, rowIndex, isPlacementTest);
            } catch (Exception e) {
                log.error("Validation error at row {}: {}", rowIndex, e.getMessage());
                throw e; // Rethrow to rollback transaction
            }

            // For PLACEMENT_TEST, all questions go into the single section (index 1)
            TestSection section = isPlacementTest
                    ? sectionByPart.get(1)
                    : sectionByPart.get(row.getPart());

            Question question = buildQuestion(row);

            // Handle Tags
            if (row.getTag() != null && !row.getTag().isEmpty()) {
                String tagName = row.getTag();
                Tag tag = tagCache.get(tagName);
                if (tag == null) {
                    tag = tagRepository.findByName(tagName)
                            .orElseGet(() -> {
                                Tag newTag = Tag.builder()
                                        .name(tagName)
                                        .tagGroup(TagGroup.ENGLISH_TOEIC)
                                        .build();
                                return tagRepository.save(newTag);
                            });
                    tagCache.put(tagName, tag);
                }
                question.setTag(tag);
            }

            // For PLACEMENT_TEST: all questions are standalone (no groups)
            if (isPlacementTest || row.getGroupCode() == null) {
                section.addQuestion(question);
            } else {
                // Grouped question (Part 3, 4, 6, 7) — TOEIC only
                String mapKey = row.getPart() + ":" + row.getGroupCode();
                QuestionGroup group = groupMap.get(mapKey);

                if (group == null) {
                    group = QuestionGroup.builder()
                            .importCode(row.getGroupCode())
                            .sharedContent(row.getPassageContent())
                            .build();
                    section.addQuestionGroup(group);
                    groupMap.put(mapKey, group);
                } else {
                    // Smart Merge for Part 7 (Double/Triple Passages)
                    String newPassage = row.getPassageContent();
                    String currentContent = group.getSharedContent();

                    if (newPassage != null && !newPassage.trim().isEmpty()) {
                        if (currentContent == null || currentContent.trim().isEmpty()) {
                            group.setSharedContent(newPassage);
                        } else if (!currentContent.contains(newPassage.trim())) {
                            // Append with double newline if it's a new unique passage
                            group.setSharedContent(currentContent + "\n\n" + newPassage);
                        }
                    }
                }

                group.addQuestion(question);
                question.setSection(section);
            }

            totalQuestions++;
        }

        totalGroups = groupMap.size();

        Test saved = testRepository.save(test);
        log.info("TOEIC import complete – testId: {}, questions: {}, groups: {}",
                saved.getId(), totalQuestions, totalGroups);

        if (testSet.getType() == TestType.PLACEMENT_TEST) {
            log.info("🔄 [PLACEMENT TEST] Reloading CAT questions bank cache...");
            questionBankCacheService.loadPlacementQuestionsToCache();
        }
        
        eventPublisher.publishEvent(new com.echill.event.TestSetUpdatedEvent(testSetId));

        return ImportTestResponse.builder()
                .testId(saved.getId())
                .testSetId(testSetId)
                .testTitle(saved.getTitle())
                .totalQuestions(totalQuestions)
                .totalGroups(totalGroups)
                .status("SUCCESS")
                .build();
    }

    // -------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------

    private Question buildQuestion(ToeicExcelRowDto row) {
        Question question = Question.builder()
                .content(row.getQuestionContent())
                .explanation(row.getExplanation())
                .orderIndex(row.getQuestionNo())
                .difficultyLevel(row.getDifficulty() != null ? row.getDifficulty() : 3)
                .build();

        String correct = row.getCorrectAnswer(); // Already "A", "B", "C", or "D"

        addAnswer(question, row.getOptionA(), "A".equals(correct));
        addAnswer(question, row.getOptionB(), "B".equals(correct));
        addAnswer(question, row.getOptionC(), "C".equals(correct));
        addAnswer(question, row.getOptionD(), "D".equals(correct));

        return question;
    }

    private void addAnswer(Question question, String content, boolean isCorrect) {
        Answer answer = Answer.builder()
                .content(content)
                .isCorrect(isCorrect)
                .build();
        question.addAnswer(answer);
    }

    private void validateRow(ToeicExcelRowDto row, int index, boolean isPlacementTest) {
        // Part validation: TOEIC requires Part 1–7; PLACEMENT_TEST allows any part value
        if (!isPlacementTest && (row.getPart() < 1 || row.getPart() > 7)) {
            throw new AppException(TeacherErrorEnum.INVALID_EXCEL_FORMAT, "Invalid Part at row " + index);
        }

        if (row.getQuestionContent() == null || row.getQuestionContent().trim().isEmpty()) {
            throw new AppException(TeacherErrorEnum.INVALID_EXCEL_FORMAT, "Missing Question Content at row " + index);
        }

        String correct = row.getCorrectAnswer();
        if (correct == null || !correct.matches("^[A-D]$")) {
            throw new AppException(TeacherErrorEnum.INVALID_EXCEL_FORMAT, "Invalid Correct Answer (must be A, B, C, or D) at row " + index);
        }

        if (row.getOptionA() == null || row.getOptionB() == null || row.getOptionC() == null) {
            throw new AppException(TeacherErrorEnum.INVALID_EXCEL_FORMAT, "Missing Options (A, B, or C) at row " + index);
        }

        // PLACEMENT_TEST allows only 3 options (A, B, C); TOEIC Part 2 also allows 3 options
        if (!isPlacementTest && row.getPart() != 2 && row.getOptionD() == null) {
            throw new AppException(TeacherErrorEnum.INVALID_EXCEL_FORMAT, "Missing Option D at row " + index);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(TeacherErrorEnum.FILE_EMPTY);
        }
        String contentType = file.getContentType();
        String fileName    = file.getOriginalFilename();
        if (!EXCEL_MIME.equals(contentType) || fileName == null || !fileName.endsWith(".xlsx")) {
            throw new AppException(TeacherErrorEnum.INVALID_FILE_TYPE);
        }
    }
}
