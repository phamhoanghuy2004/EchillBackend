package com.echill.dto.exel;

import com.echill.entity.enums.SkillType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExcelQuestionDto {
    private String content;
    private String explanation;
    private SkillType skillType;
    private String tagName;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctAnswer;
    private int rowNumber;
}
