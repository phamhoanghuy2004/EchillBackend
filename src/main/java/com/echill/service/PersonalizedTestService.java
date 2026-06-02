package com.echill.service;

import com.echill.dto.ai.AiQuestionDto;
import com.echill.dto.ai.AiQuestionListResponse;
import com.echill.dto.response.guest.TestPracticeResponse;
import com.echill.entity.*;
import com.echill.entity.enums.*;
import com.echill.exception.AppException;
import com.echill.exception.StudentErrorEnum;
import com.echill.mapper.TestPracticeMapper;
import com.echill.repository.*;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonalizedTestService {

    static final String PERSONALIZED_TEST_SET_TITLE = "Personalized Practice Set";
    static final int TARGET_QUESTION_COUNT = 10;
    static final int MAX_WEAK_TAGS = 3;

    UserSkillProfileRepository profileRepository;
    StudentProfileRepository studentProfileRepository;
    TestSetRepository testSetRepository;
    TestRepository testRepository;
    QuestionRepository questionRepository;
    UserRepository userRepository;
    TestPracticeMapper testPracticeMapper;
    TestSessionRepository testSessionRepository;
    UserSkillProfileService profileService;

    ChatClient.Builder chatClientBuilder;

    @Lazy
    @Autowired
    TestService testService;

    @Transactional
    public TestPracticeResponse generatePersonalizedTest() {
        Long userId = SecurityUtils.getCurrentUserId();

        StudentProfile studentProfile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.PROFILE_NOT_FOUND));

        int safetyGateLevel = resolveSafetyGateLevel(studentProfile.getLevel());

        UserSkillProfile weakTag = profileService.findTopKnowledgeGap(userId)
                .orElseThrow(() -> new AppException(StudentErrorEnum.NO_SKILL_DATA_FOR_PERSONALIZED));

        TestSet testSet = getOrCreatePersonalizedTestSet(userId);

        Optional<TestSession> activeSession = testSessionRepository
                .findFirstByStudentIdAndTestSetIdAndStatusOrderByCreatedAtDesc(
                        userId, testSet.getId(), TestSessionStatus.IN_PROGRESS);

        if (activeSession.isPresent()) {
            return testService.resumePracticeSession(activeSession.get());
        }

        Test savedTest = buildAndSaveTest(testSet, weakTag, safetyGateLevel);
        // Dùng entity vừa build trong memory — tránh MultipleBagFetch khi reload sections+questions+answers
        TestPracticeResponse practiceResponse = testPracticeMapper.toPracticeResponse(savedTest);
        return testService.startPracticeSession(userId, testSet.getId(), savedTest.getId(), practiceResponse);
    }

    private TestSet getOrCreatePersonalizedTestSet(Long userId) {
        return testSetRepository.findByUserIdAndTitle(userId, PERSONALIZED_TEST_SET_TITLE)
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    TestSet testSet = TestSet.builder()
                            .title(PERSONALIZED_TEST_SET_TITLE)
                            .description("Bộ đề luyện tập cá nhân hóa do AI sinh theo lỗ hổng kiến thức")
                            .type(TestType.PRACTICE)
                            .isPublic(true)
                            .year(LocalDateTime.now().getYear())
                            .user(user)
                            .build();
                    return testSetRepository.save(testSet);
                });
    }

    private String resolveSkillCategoryKey(Tag tag) {
        String name = resolveSkillCategoryName(tag).toLowerCase();
        if (name.contains("grammar") || name.contains("ngữ pháp")) {
            return "grammar";
        }
        if (name.contains("vocabulary") || name.contains("từ vựng")) {
            return "vocabulary";
        }
        if (name.contains("reading") || name.contains("đọc")) {
            return "reading";
        }
        if (isListeningSkillCategory(name)) {
            return "listening";
        }
        return "other";
    }

    private int resolveSafetyGateLevel(Level level) {
        return switch (level) {
            case BEGINNER, UNDETERMINED -> 2;
            case INTERMEDIATE -> 4;
            case ADVANCED -> 5;
        };
    }

    private Test buildAndSaveTest(TestSet testSet, UserSkillProfile weakTag, int safetyGateLevel) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        Test test = Test.builder()
                .title("Luyện tập cá nhân hóa - " + timestamp)
                .type(TestType.PRACTICE)
                .durationMinutes(30)
                .passScore(60.0)
                .testSet(testSet)
                .build();

        TestSection section = TestSection.builder()
                .title("Luyện tập theo điểm yếu")
                .orderIndex(1)
                .instructions("Hoàn thành " + TARGET_QUESTION_COUNT + " câu hỏi được chọn theo kỹ năng bạn cần cải thiện.")
                .build();
        test.addSection(section);

        int questionsForTag = TARGET_QUESTION_COUNT;
        Set<Long> usedSourceQuestionIds = new HashSet<>();
        int orderIndex = 1;
        boolean geminiQuotaExceeded = false;
        Tag tag = weakTag.getTag();

        if (usesListeningBank(tag)) {
            for (int q = 0; q < questionsForTag; q++) {
                int difficulty = pickDifficulty(weakTag.getCurrentLevel(), safetyGateLevel);
                orderIndex = addListeningQuestion(section, tag, difficulty, usedSourceQuestionIds, orderIndex);
            }
        } else {
            // Grammar, Vocabulary, Reading (và mọi nhánh không phải Listening) → chỉ Gemini AI
            var readingResult = addReadingQuestionsForTag(
                    section, tag, weakTag, questionsForTag, safetyGateLevel, orderIndex);
            orderIndex = readingResult.orderIndex();
            geminiQuotaExceeded = readingResult.quotaExceeded();
        }

        if (section.getQuestions().isEmpty()) {
            if (geminiQuotaExceeded) {
                throw new AppException(StudentErrorEnum.GEMINI_QUOTA_EXCEEDED,
                        "Hạn mức Gemini API đã hết (lỗi 429). Hãy đợi khoảng 1 phút rồi thử lại, hoặc kiểm tra gói free tier / billing tại Google AI Studio.");
            }
            throw new AppException(StudentErrorEnum.PERSONALIZED_TEST_GENERATION_FAILED,
                    "Không thể tạo câu hỏi cho đề luyện tập. Vui lòng thử lại sau.");
        }

        return testRepository.save(test);
    }

    private int pickDifficulty(int currentLevel, int safetyGateLevel) {
        int challengeLevel = Math.min(currentLevel + 1, safetyGateLevel);
        return ThreadLocalRandom.current().nextInt(100) < 80 ? currentLevel : challengeLevel;
    }

    /**
     * Chỉ nhánh Listening lấy câu từ ngân hàng đề (Part 1–4, audio/group).
     * Grammar, Vocabulary, Reading và các tag con khác → Gemini AI (không clone DB).
     */
    private boolean usesListeningBank(Tag tag) {
        String category = resolveSkillCategoryName(tag).toLowerCase();
        if (isReadingSkillCategory(category)) {
            return false;
        }
        return isListeningSkillCategory(category);
    }

    private String resolveSkillCategoryName(Tag tag) {
        if (tag.getParent() != null) {
            return tag.getParent().getName();
        }
        return tag.getName();
    }

    private boolean isListeningSkillCategory(String name) {
        return name.contains("listening")
                || name.contains("nghe")
                || name.contains("part 1")
                || name.contains("part 2")
                || name.contains("part 3")
                || name.contains("part 4");
    }

    private boolean isReadingSkillCategory(String name) {
        return name.contains("reading")
                || name.contains("đọc")
                || name.contains("grammar")
                || name.contains("vocabulary")
                || name.contains("ngữ pháp")
                || name.contains("từ vựng")
                || name.contains("part 5")
                || name.contains("part 6")
                || name.contains("part 7");
    }

    private int addListeningQuestion(TestSection section, Tag tag, int difficulty,
                                     Set<Long> usedSourceIds, int orderIndex) {
        Question source = findListeningSource(tag.getId(), difficulty, usedSourceIds);
        if (source == null) {
            log.warn("No listening question found for tag {} at difficulty {}", tag.getName(), difficulty);
            return orderIndex;
        }

        Question fullSource = questionRepository.findByIdForClone(source.getId())
                .orElse(source);

        if (fullSource.getQuestionGroup() != null) {
            List<Question> groupQuestions = loadGroupQuestionsWithAnswers(fullSource.getQuestionGroup().getId());
            return cloneQuestionGroup(section, fullSource.getQuestionGroup(), groupQuestions, usedSourceIds, orderIndex);
        }

        Question cloned = cloneQuestion(fullSource, section, orderIndex);
        section.addQuestion(cloned);
        usedSourceIds.add(fullSource.getId());
        return orderIndex + 1;
    }

    private record ReadingAddResult(int orderIndex, boolean quotaExceeded) {
    }

    private ReadingAddResult addReadingQuestionsForTag(TestSection section, Tag tag, UserSkillProfile profile,
                                                     int count, int safetyGateLevel, int orderIndex) {
        List<Integer> difficulties = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            difficulties.add(pickDifficulty(profile.getCurrentLevel(), safetyGateLevel));
        }

        var aiResult = tryGenerateReadingBatch(tag, difficulties);
        int added = 0;
        for (AiQuestionDto dto : aiResult.questions()) {
            if (added >= count) {
                break;
            }
            Question question = buildQuestionFromAi(dto, tag, difficulties.get(added), section, orderIndex);
            section.addQuestion(question);
            orderIndex++;
            added++;
        }

        if (added < count) {
            log.warn("AI chỉ sinh được {}/{} câu cho tag {} (category: {}). Không dùng câu DB làm fallback.",
                    added, count, tag.getName(), resolveSkillCategoryName(tag));
        }

        return new ReadingAddResult(orderIndex, aiResult.quotaExceeded() && added == 0);
    }

    private record AiBatchResult(List<AiQuestionDto> questions, boolean quotaExceeded) {
        static AiBatchResult empty(boolean quotaExceeded) {
            return new AiBatchResult(List.of(), quotaExceeded);
        }
    }

    private AiBatchResult tryGenerateReadingBatch(Tag tag, List<Integer> difficulties) {
        if (difficulties.isEmpty()) {
            return AiBatchResult.empty(false);
        }
        try {
            AiQuestionListResponse aiResponse = chatClientBuilder.build()
                    .prompt()
                    .user(buildReadingBatchPrompt(tag, difficulties))
                    .call()
                    .entity(AiQuestionListResponse.class);

            if (aiResponse == null || aiResponse.questions() == null || aiResponse.questions().isEmpty()) {
                log.warn("AI returned empty questions for tag {}", tag.getName());
                return AiBatchResult.empty(false);
            }
            return new AiBatchResult(aiResponse.questions(), false);
        } catch (Exception e) {
            boolean quotaExceeded = isGeminiQuotaError(e);
            if (quotaExceeded) {
                log.warn("Gemini quota/rate limit for tag {} (category: {}): {}",
                        tag.getName(), resolveSkillCategoryName(tag), extractErrorMessage(e));
            } else {
                log.error("Failed to generate reading questions via AI for tag {}", tag.getName(), e);
            }
            return AiBatchResult.empty(quotaExceeded);
        }
    }

    private String buildReadingBatchPrompt(Tag tag, List<Integer> difficulties) {
        String parentName = tag.getParent() != null ? tag.getParent().getName() : "";
        String tagName = tag.getName();
        String fullCategory = (parentName + " " + tagName).toLowerCase();

        String difficultyList = difficulties.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(", "));

        String formatInstruction;

        if (fullCategory.contains("part 7")) {
            formatInstruction = """
                    Generate exactly %d TOEIC Part 7 (Reading Comprehension) multiple-choice questions.
                    For each question, create a short reading passage (e.g., email, memo, notice) and 1 related question.
                    Combine the passage and the question into the 'content' field in this format:
                    [Passage Text]
                    
                    [Question Text]
                    """.formatted(difficulties.size());
        } else if (fullCategory.contains("part 5") || fullCategory.contains("part 6")) {
            formatInstruction = """
                    Generate exactly %d TOEIC Incomplete Sentences/Texts multiple-choice questions.
                    For each question, provide a sentence or short text with a blank space (represented by "___").
                    """.formatted(difficulties.size());
        } else {
            // Default to Part 5 style if not explicitly part 7
            formatInstruction = """
                    Generate exactly %d TOEIC-style multiple-choice questions (similar to Part 5 Incomplete Sentences).
                    For each question, provide a sentence with a blank space (represented by "___").
                    """.formatted(difficulties.size());
        }

        return """
                %s
                Topic tag: %s (parent skill: %s).
                Each question should match one of these difficulty levels (1=easiest, 5=hardest), in order: %s.
                Provide exactly 4 options A, B, C, D and one correct answer letter per question.
                Return JSON with a "questions" array containing exactly %d items.
                Fields per question: content, optionA, optionB, optionC, optionD, correctAnswer (A/B/C/D), explanation (brief, in Vietnamese).
                """.formatted(formatInstruction, tagName, parentName, difficultyList, difficulties.size());
    }

    private boolean isGeminiQuotaError(Throwable e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("429")
                        || lower.contains("quota")
                        || lower.contains("rate limit")
                        || lower.contains("resource_exhausted")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String extractErrorMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : e.getMessage();
    }

    private Question buildQuestionFromAi(AiQuestionDto dto, Tag tag, int difficulty,
                                         TestSection section, int orderIndex) {
        Question question = Question.builder()
                .content(dto.content())
                .explanation(dto.explanation())
                .skillType(SkillType.READING)
                .tag(tag)
                .difficultyLevel(difficulty)
                .orderIndex(orderIndex)
                .section(section)
                .build();

        addAnswer(question, dto.optionA(), "A".equalsIgnoreCase(dto.correctAnswer()));
        addAnswer(question, dto.optionB(), "B".equalsIgnoreCase(dto.correctAnswer()));
        addAnswer(question, dto.optionC(), "C".equalsIgnoreCase(dto.correctAnswer()));
        if (dto.optionD() != null && !dto.optionD().isBlank()) {
            addAnswer(question, dto.optionD(), "D".equalsIgnoreCase(dto.correctAnswer()));
        }

        return question;
    }

    private Question findListeningSource(Long tagId, int difficulty, Set<Long> usedSourceIds) {
        List<Question> pool = questionRepository
                .findListeningQuestionsByTagAndDifficulty(tagId, SkillType.LISTENING, difficulty);

        if (pool.isEmpty()) {
            pool = questionRepository.findListeningQuestionsByTag(tagId, SkillType.LISTENING);
        }

        List<Question> available = pool.stream()
                .filter(q -> !usedSourceIds.contains(q.getId()))
                .toList();

        if (available.isEmpty()) {
            return null;
        }

        return available.get(ThreadLocalRandom.current().nextInt(available.size()));
    }

    private List<Question> loadGroupQuestionsWithAnswers(Long groupId) {
        List<Question> refs = questionRepository.findByQuestionGroupIdOrderByOrderIndex(groupId);
        if (refs.isEmpty()) {
            return List.of();
        }
        List<Long> ids = refs.stream().map(Question::getId).toList();
        var loadedById = questionRepository.findByIdsWithAnswers(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
        return ids.stream()
                .map(loadedById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private int cloneQuestionGroup(TestSection section, QuestionGroup sourceGroup,
                                   List<Question> groupQuestions,
                                   Set<Long> usedSourceIds, int orderIndex) {
        QuestionGroup clonedGroup = QuestionGroup.builder()
                .importCode(sourceGroup.getImportCode())
                .sharedContent(sourceGroup.getSharedContent())
                .sharedAudioUrl(sourceGroup.getSharedAudioUrl())
                .sharedAudioPublicId(sourceGroup.getSharedAudioPublicId())
                .sharedImageUrl(sourceGroup.getSharedImageUrl())
                .sharedImagePublicId(sourceGroup.getSharedImagePublicId())
                .section(section)
                .build();
        section.addQuestionGroup(clonedGroup);

        for (Question sourceQ : groupQuestions) {
            Question cloned = cloneQuestion(sourceQ, section, orderIndex);
            cloned.setQuestionGroup(clonedGroup);
            clonedGroup.addQuestion(cloned);
            section.addQuestion(cloned);
            usedSourceIds.add(sourceQ.getId());
            orderIndex++;
        }

        return orderIndex;
    }

    private Question cloneQuestion(Question source, TestSection section, int orderIndex) {
        Question cloned = Question.builder()
                .content(source.getContent())
                .audioUrl(source.getAudioUrl())
                .audioPublicId(source.getAudioPublicId())
                .imageUrl(source.getImageUrl())
                .imagePublicId(source.getImagePublicId())
                .explanation(source.getExplanation())
                .skillType(source.getSkillType())
                .difficultyLevel(source.getDifficultyLevel())
                .orderIndex(orderIndex)
                .tag(source.getTag())
                .section(section)
                .build();

        for (Answer sourceAnswer : source.getAnswers()) {
            addAnswer(cloned, sourceAnswer.getContent(), Boolean.TRUE.equals(sourceAnswer.getIsCorrect()));
        }

        return cloned;
    }

    private void addAnswer(Question question, String content, boolean isCorrect) {
        Answer answer = Answer.builder()
                .content(content)
                .isCorrect(isCorrect)
                .build();
        question.addAnswer(answer);
    }
}
