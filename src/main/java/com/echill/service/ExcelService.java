package com.echill.service;

import com.echill.entity.Answer;
import com.echill.entity.Question;
import com.echill.entity.Tag;
import com.echill.entity.enums.SkillType;
import com.echill.exception.AppException;
import com.echill.exception.TeacherErrorEnum;
import com.echill.repository.TagRepository;
import com.github.pjfanning.xlsx.StreamingReader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExcelService {
    TagRepository tagRepository;

    @Getter
    @AllArgsConstructor
    public enum ExcelColumn {
        QUESTION(0),
        A(1),
        B(2),
        C(3),
        D(4),
        CORRECT(5),
        EXPLANATION(6),
        SKILL(7),
        TAG(8);

        private final int index;
    }

    public enum AnswerOption {
        A, B, C, D
    }

    public List<Question> parseExcelToQuestions(MultipartFile file) {
        Map<String, Tag> tagCache = new HashMap<>();
        List<Question> questions = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = StreamingReader.builder()
                     .rowCacheSize(100)
                     .bufferSize(4096)
                     .open(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row row = rows.next();
                if (isRowEmpty(row)) continue;

                try {
                    Question question = parseRowToQuestion(row,tagCache);
                    if (question != null) {
                        questions.add(question);
                    }
                } catch (AppException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("Error parsing row {}: {}", row.getRowNum() + 1, e.getMessage());
                    throw new AppException(TeacherErrorEnum.EXCEL_PARSE_ERROR);
                }
            }
        } catch (IOException e) {
            log.error("IO Error while processing Excel: {}", e.getMessage());
            throw new AppException(TeacherErrorEnum.EXCEL_PARSE_ERROR);
        }

        return questions;
    }

    private Question parseRowToQuestion(Row row, Map<String, Tag> tagCache) {
        String content = getCellValue(row, ExcelColumn.QUESTION);
        String a = getCellValue(row, ExcelColumn.A);
        String b = getCellValue(row, ExcelColumn.B);
        String c = getCellValue(row, ExcelColumn.C);
        String d = getCellValue(row, ExcelColumn.D);
        String correct = getCellValue(row, ExcelColumn.CORRECT);
        String explanation = getCellValue(row, ExcelColumn.EXPLANATION);
        String skillStr = getCellValue(row, ExcelColumn.SKILL);
        String tagName = getCellValue(row, ExcelColumn.TAG);

        // Validation: Required A, B, C and Question
        if (isEmpty(content) || isEmpty(a) || isEmpty(b) || isEmpty(c)) {
            log.warn("Skipping row {} due to missing required fields", row.getRowNum() + 1);
            return null;
        }

        SkillType skillType;
        try {
            skillType = SkillType.valueOf(skillStr.toUpperCase().trim());
        } catch (Exception e) {
            log.error("Invalid skill type '{}' at row {}", skillStr, row.getRowNum() + 1);
            throw new AppException(TeacherErrorEnum.INVALID_SKILL_TYPE);
        }

        // Tag Handling (Basic caching can be improved later if needed)

        Tag tag = null;
        if (!isEmpty(tagName)) {
            String cleanTagName = tagName.trim().toLowerCase();
            tag = tagCache.computeIfAbsent(cleanTagName, name ->
                    tagRepository.findByName(name)
                            .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()))
            );
        }

        // Question construction
        Question question = Question.builder()
                .content(content.trim())
                .explanation(explanation != null ? explanation.trim() : "")
                .skillType(skillType)
                .tag(tag)
                .build();

        if (isEmpty(correct)) {
            throw new AppException(TeacherErrorEnum.MISSING_CORRECT_ANSWER_COLUMN);
        }
        
        String cleanCorrect = correct.toUpperCase().trim();
        AnswerOption correctOption;
        try {
            correctOption = AnswerOption.valueOf(cleanCorrect);
        } catch (Exception e) {
            throw new AppException(TeacherErrorEnum.INVALID_CORRECT_ANSWER);
        }

        addAnswer(question, a.trim(), correctOption == AnswerOption.A);
        addAnswer(question, b.trim(), correctOption == AnswerOption.B);
        addAnswer(question, c.trim(), correctOption == AnswerOption.C);
        
        if (!isEmpty(d)) {
            addAnswer(question, d.trim(), correctOption == AnswerOption.D);
        } else if (correctOption == AnswerOption.D) {
            log.error("Correct answer is 'D' but Option D is empty at row {}", row.getRowNum() + 1);
            throw new AppException(TeacherErrorEnum.INVALID_CORRECT_ANSWER);
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

    private String getCellValue(Row row, ExcelColumn column) {
        Cell cell = row.getCell(column.getIndex());
        if (cell == null) return null;
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    private boolean isEmpty(String str) {
        return str == null || str.isBlank();
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}
