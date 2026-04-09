package com.echill.service.persistence;

import com.echill.dto.exel.ExcelQuestionDto;
import com.echill.dto.request.TestRequest;
import com.echill.dto.response.TestResponse;
import com.echill.entity.*;
import com.echill.entity.enums.AnswerOption;
import com.echill.exception.AppException;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.TestMapper;
import com.echill.repository.TagRepository;
import com.echill.repository.TestRepository;
import com.echill.repository.TestSetRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestPersistService {

    TestRepository testRepository;
    TestSetRepository testSetRepository;
    TagRepository tagRepository;
    TestMapper testMapper;

    @Value("${app.test.default-section-name:Default Section}")
    @NonFinal
    String defaultSectionName;

    @Transactional
    public TestResponse saveTestTransaction(TestRequest request, List<ExcelQuestionDto> dtoList) {
        log.info("Persisting Test [{}] with {} questions", request.getTitle(), dtoList.size());

        TestSet testSet = testSetRepository.findById(request.getTestSetId())
                .orElseThrow(() -> {
                    log.error("Failed to save test [{}]. TestSet ID {} not found", request.getTitle(), request.getTestSetId());
                    return new AppException(TeacherErrorEnum.TEST_SET_NOT_FOUND);
                });

        Map<String, Tag> tagMap = preloadAndSaveTags(dtoList);

        Test test = Test.builder()
                .title(request.getTitle())
                .type(request.getType())
                .durationMinutes(request.getDurationMinutes())
                .passScore(request.getPassScore())
                .testSet(testSet)
                .build();

        TestSection defaultSection = TestSection.builder()
                .title(defaultSectionName) // Lấy từ biến config
                .orderIndex(1)
                .build();
        test.addSection(defaultSection);

        int currentOrderIndex = 1;
        for (ExcelQuestionDto dto : dtoList) {
            Question question = buildQuestionEntity(dto, tagMap, currentOrderIndex++);
            defaultSection.addQuestion(question);
        }

        Test savedTest = testRepository.save(test);
        log.info("Successfully persisted Test [{}] with ID {}", savedTest.getTitle(), savedTest.getId());
        return testMapper.toResponse(savedTest);
    }

    private Map<String, Tag> preloadAndSaveTags(List<ExcelQuestionDto> dtoList) {
        Set<String> tagNames = dtoList.stream()
                .map(ExcelQuestionDto::getTagName)
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toLowerCase())
                .collect(Collectors.toSet());

        if (tagNames.isEmpty()) return new HashMap<>();

        Map<String, Tag> existingTags = tagRepository.findAllByNameIn(tagNames).stream()
                .collect(Collectors.toMap(
                        t -> t.getName().toLowerCase(),
                        t -> t,
                        (existing, replacement) -> existing
                ));

        List<Tag> newTags = tagNames.stream()
                .filter(name -> !existingTags.containsKey(name))
                .map(name -> Tag.builder().name(name).build())
                .collect(Collectors.toList());

        if (!newTags.isEmpty()) {
            List<Tag> savedTags = tagRepository.saveAll(newTags);
            savedTags.forEach(t -> existingTags.put(t.getName().toLowerCase(), t));
        }

        return existingTags;
    }

    private Question buildQuestionEntity(ExcelQuestionDto dto, Map<String, Tag> tagMap, int orderIndex) {
        String normalizedTagName = dto.getTagName() != null ? dto.getTagName().trim().toLowerCase() : null;
        Tag tag = normalizedTagName != null ? tagMap.get(normalizedTagName) : null;

        Question question = Question.builder()
                .content(dto.getContent())
                .explanation(dto.getExplanation())
                .skillType(dto.getSkillType())
                .tag(tag)
                .orderIndex(orderIndex)
                .build();

        String correct = dto.getCorrectAnswer();

        addAnswer(question, dto.getOptionA(), AnswerOption.A.name().equals(correct));
        addAnswer(question, dto.getOptionB(), AnswerOption.B.name().equals(correct));
        addAnswer(question, dto.getOptionC(), AnswerOption.C.name().equals(correct));

        if (dto.getOptionD() != null) {
            addAnswer(question, dto.getOptionD(), AnswerOption.D.name().equals(correct));
        }

        return question;
    }

    private void addAnswer(Question question, String content, boolean isCorrect) {
        Answer answer = Answer.builder()
                .content(content)
                .isCorrect(isCorrect)
                .build();
        question.addAnswer(answer);
    }
}