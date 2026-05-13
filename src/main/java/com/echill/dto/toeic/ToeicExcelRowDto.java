package com.echill.dto.toeic;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a single row parsed from the TOEIC import Excel file.
 * Columns: part | group_code | question_no | question_content |
 *          option_a | option_b | option_c | option_d |
 *          correct_answer | explanation | passage_content
 */
@Data
@Builder
public class ToeicExcelRowDto {
    private int rowNumber;
    private int part;              // 1–7
    private String groupCode;      // nullable — null for standalone questions
    private int questionNo;
    private String questionContent;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctAnswer;  // A / B / C / D
    private String explanation;
    private String passageContent; // shared content for grouped questions
    private String tag;            // optional tag for the question
}
