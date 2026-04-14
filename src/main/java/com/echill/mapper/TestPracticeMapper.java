package com.echill.mapper;

import com.echill.dto.response.guest.*;
import com.echill.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TestPracticeMapper {

    @Mapping(source = "testSet.id", target = "testSetId")
    TestPracticeResponse toPracticeResponse(Test test);

    TestSectionPracticeResponse toSectionPracticeResponse(TestSection section);

    @Mapping(source = "tag.name", target = "tagName")
    @Mapping(source = "questionGroup", target = "group")
    QuestionPracticeResponse toQuestionPracticeResponse(Question question);

    @Mapping(source = "isCorrect", target = "isCorrect")
    AnswerPracticeResponse toAnswerPracticeResponse(Answer answer);

    QuestionGroupPracticeResponse toGroupPracticeResponse(QuestionGroup group);

    TestPracticeResponse clonePracticeResponse(TestPracticeResponse source);
}
