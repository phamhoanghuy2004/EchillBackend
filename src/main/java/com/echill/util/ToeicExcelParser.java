package com.echill.util;

import com.echill.dto.toeic.ToeicExcelRowDto;
import com.echill.exception.AppException;
import com.echill.exception.TeacherErrorEnum;
import com.github.pjfanning.xlsx.StreamingReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Parses TOEIC Excel files into ToeicExcelRowDto objects.
 *
 * Expected column layout (0-indexed):
 *  0 – part             (1–7)
 *  1 – group_code       (nullable)
 *  2 – question_no
 *  3 – question_content
 *  4 – option_a
 *  5 – option_b
 *  6 – option_c
 *  7 – option_d
 *  8 – correct_answer   (A/B/C/D)
 *  9 – explanation      (optional)
 * 10 – passage_content  (optional, shared for group)
 * 11 – tag              (optional)
 */
@Component
@Slf4j
public class ToeicExcelParser {

    private static final DataFormatter FORMATTER = new DataFormatter();

    // Column indices
    private static final int COL_PART            = 0;
    private static final int COL_GROUP_CODE      = 1;
    private static final int COL_QUESTION_NO     = 2;
    private static final int COL_QUESTION_CONTENT = 3;
    private static final int COL_OPTION_A        = 4;
    private static final int COL_OPTION_B        = 5;
    private static final int COL_OPTION_C        = 6;
    private static final int COL_OPTION_D        = 7;
    private static final int COL_CORRECT_ANSWER  = 8;
    private static final int COL_EXPLANATION     = 9;
    private static final int COL_PASSAGE_CONTENT = 10;
    private static final int COL_TAG             = 11;

    public List<ToeicExcelRowDto> parse(MultipartFile file) {
        List<ToeicExcelRowDto> rows = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = StreamingReader.builder()
                     .rowCacheSize(100)
                     .bufferSize(4096)
                     .open(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // Skip header row
            if (rowIterator.hasNext()) rowIterator.next();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (isRowEmpty(row)) continue;

                try {
                    rows.add(parseRow(row));
                } catch (AppException e) {
                    throw e; // propagate validation errors
                } catch (Exception e) {
                    log.error("Error parsing TOEIC Excel row {}", row.getRowNum() + 1, e);
                    throw new AppException(TeacherErrorEnum.EXCEL_PARSE_ERROR);
                }
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("IO error reading TOEIC Excel file", e);
            throw new AppException(TeacherErrorEnum.EXCEL_PARSE_ERROR);
        }

        return rows;
    }

    private ToeicExcelRowDto parseRow(Row row) {
        int rowNum = row.getRowNum() + 1;

        String partStr          = getCellValue(row, COL_PART);
        String groupCode        = getCellValue(row, COL_GROUP_CODE);
        String questionNoStr    = getCellValue(row, COL_QUESTION_NO);
        String questionContent  = getCellValue(row, COL_QUESTION_CONTENT);
        String optionA          = getCellValue(row, COL_OPTION_A);
        String optionB          = getCellValue(row, COL_OPTION_B);
        String optionC          = getCellValue(row, COL_OPTION_C);
        String optionD          = getCellValue(row, COL_OPTION_D);
        String correctAnswer    = getCellValue(row, COL_CORRECT_ANSWER);
        String explanation      = getCellValue(row, COL_EXPLANATION);
        String passageContent   = getCellValue(row, COL_PASSAGE_CONTENT);
        String tag              = getCellValue(row, COL_TAG);

        // Validate required fields
        if (isEmpty(partStr) || isEmpty(questionNoStr) || isEmpty(questionContent)
                || isEmpty(optionA) || isEmpty(optionB) || isEmpty(optionC)
                || isEmpty(optionD) || isEmpty(correctAnswer)) {
            log.error("TOEIC Excel: Missing required field at row {}", rowNum);
            throw new AppException(TeacherErrorEnum.INVALID_EXCEL_FORMAT);
        }

        int part;
        try {
            part = (int) Double.parseDouble(partStr.trim());
        } catch (NumberFormatException e) {
            log.error("TOEIC Excel: Invalid part number '{}' at row {}", partStr, rowNum);
            throw new AppException(TeacherErrorEnum.INVALID_EXCEL_FORMAT);
        }

        if (part < 1 || part > 7) {
            log.error("TOEIC Excel: Part must be 1–7 at row {}", rowNum);
            throw new AppException(TeacherErrorEnum.INVALID_EXCEL_FORMAT);
        }

        int questionNo;
        try {
            questionNo = (int) Double.parseDouble(questionNoStr.trim());
        } catch (NumberFormatException e) {
            log.error("TOEIC Excel: Invalid question_no '{}' at row {}", questionNoStr, rowNum);
            throw new AppException(TeacherErrorEnum.INVALID_EXCEL_FORMAT);
        }

        String normalizedCorrect = correctAnswer.toUpperCase().trim();
        if (!normalizedCorrect.matches("[ABCD]")) {
            log.error("TOEIC Excel: correct_answer must be A/B/C/D at row {}", rowNum);
            throw new AppException(TeacherErrorEnum.INVALID_CORRECT_ANSWER);
        }

        return ToeicExcelRowDto.builder()
                .rowNumber(rowNum)
                .part(part)
                .groupCode(isEmpty(groupCode) ? null : groupCode.trim())
                .questionNo(questionNo)
                .questionContent(questionContent.trim())
                .optionA(optionA.trim())
                .optionB(optionB.trim())
                .optionC(optionC.trim())
                .optionD(optionD.trim())
                .correctAnswer(normalizedCorrect)
                .explanation(isEmpty(explanation) ? "" : explanation.trim())
                .passageContent(isEmpty(passageContent) ? null : passageContent.trim())
                .tag(isEmpty(tag) ? null : tag.trim())
                .build();
    }

    private String getCellValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        return FORMATTER.formatCellValue(cell);
    }

    private boolean isEmpty(String str) {
        return str == null || str.isBlank();
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }
}
