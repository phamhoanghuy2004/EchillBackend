package com.echill.service;

import com.echill.dto.exel.ExcelQuestionDto;
import com.echill.exception.AppException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelServiceTest {

    private final ExcelService excelService = new ExcelService();

    private MockMultipartFile createExcelFile(String difficultyStr) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            // Header row
            Row header = sheet.createRow(0);
            for (int i = 0; i <= 9; i++) {
                header.createCell(i).setCellValue("Header" + i);
            }

            // Data row
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Question content?");
            row.createCell(1).setCellValue("A");
            row.createCell(2).setCellValue("B");
            row.createCell(3).setCellValue("C");
            row.createCell(4).setCellValue("D");
            row.createCell(5).setCellValue("A");
            row.createCell(6).setCellValue("Explanation");
            row.createCell(7).setCellValue("listening"); // skill type
            row.createCell(8).setCellValue("grammar"); // tag name
            row.createCell(9).setCellValue(difficultyStr); // difficulty

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bos.toByteArray());
        }
    }

    @Test
    void parseExcelToDto_ValidDifficulty_Success() throws IOException {
        MockMultipartFile file = createExcelFile("3");
        List<ExcelQuestionDto> results = excelService.parseExcelToDto(file);
        assertEquals(1, results.size());
        assertEquals(3, results.get(0).getDifficulty());
    }

    @Test
    void parseExcelToDto_ValidDecimalDifficulty_Success() throws IOException {
        MockMultipartFile file = createExcelFile("4.0");
        List<ExcelQuestionDto> results = excelService.parseExcelToDto(file);
        assertEquals(1, results.size());
        assertEquals(4, results.get(0).getDifficulty());
    }

    @Test
    void parseExcelToDto_InvalidDifficultyRange_ThrowsException() throws IOException {
        MockMultipartFile file = createExcelFile("6");
        assertThrows(AppException.class, () -> excelService.parseExcelToDto(file));
    }

    @Test
    void parseExcelToDto_InvalidDifficultyFormat_ThrowsException() throws IOException {
        MockMultipartFile file = createExcelFile("abc");
        assertThrows(AppException.class, () -> excelService.parseExcelToDto(file));
    }
}
