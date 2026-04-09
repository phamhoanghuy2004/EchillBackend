package com.echill.service;

import com.echill.dto.exel.ExcelQuestionDto;
import com.echill.entity.enums.SkillType;
import com.echill.exception.AppException;
import com.echill.exception.TeacherErrorEnum;
import com.github.pjfanning.xlsx.StreamingReader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExcelService {

    static DataFormatter FORMATTER = new DataFormatter();

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

    public List<ExcelQuestionDto> parseExcelToDto(MultipartFile file) {
        List<ExcelQuestionDto> dtoList = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = StreamingReader.builder()
                     .rowCacheSize(100)
                     .bufferSize(4096)
                     .open(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            if (rows.hasNext()) rows.next();

            while (rows.hasNext()) {
                Row row = rows.next();
                if (isRowEmpty(row)) continue;

                try {
                    dtoList.add(parseRow(row));
                } catch (AppException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("Error parsing Excel at row {}", row.getRowNum() + 1, e);
                    throw new AppException(TeacherErrorEnum.EXCEL_PARSE_ERROR);
                }
            }
        } catch (Exception e) {
            log.error("IO Error reading file", e);
            throw new AppException(TeacherErrorEnum.EXCEL_PARSE_ERROR);
        }

        return dtoList;
    }

    private ExcelQuestionDto parseRow(Row row) {
        int rowNum = row.getRowNum() + 1;

        String content = getCellValue(row, ExcelColumn.QUESTION);
        String a = getCellValue(row, ExcelColumn.A);
        String b = getCellValue(row, ExcelColumn.B);
        String c = getCellValue(row, ExcelColumn.C);
        String d = getCellValue(row, ExcelColumn.D);
        String correct = getCellValue(row, ExcelColumn.CORRECT);
        String explanation = getCellValue(row, ExcelColumn.EXPLANATION);
        String skillStr = getCellValue(row, ExcelColumn.SKILL);
        String tagName = getCellValue(row, ExcelColumn.TAG);

        if (isEmpty(content) || isEmpty(a) || isEmpty(b) || isEmpty(correct) || isEmpty(skillStr)) {
            log.error("Missing required fields at row {}", rowNum);
            throw new AppException(TeacherErrorEnum.INVALID_EXCEL_FORMAT); // Nên customize exception để trả về rowNum
        }

        SkillType skillType;
        try {
            skillType = SkillType.valueOf(skillStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            log.error("Invalid SkillType '{}' at row {}", skillStr, rowNum);
            throw new AppException(TeacherErrorEnum.INVALID_SKILL_TYPE);
        }

        return ExcelQuestionDto.builder()
                .content(content.trim())
                .optionA(a.trim())
                .optionB(b.trim())
                .optionC(c.trim())
                .optionD(isEmpty(d) ? null : d.trim())
                .correctAnswer(correct.toUpperCase().trim())
                .explanation(isEmpty(explanation) ? "" : explanation.trim())
                .skillType(skillType)
                .tagName(isEmpty(tagName) ? null : tagName.trim().toLowerCase())
                .rowNumber(rowNum)
                .build();
    }

    private String getCellValue(Row row, ExcelColumn column) {
        Cell cell = row.getCell(column.getIndex());
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