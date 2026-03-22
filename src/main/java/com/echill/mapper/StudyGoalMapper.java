package com.echill.mapper;

import com.echill.dto.request.StudyGoalRequest;
import com.echill.dto.response.StudyGoalResponse;
import com.echill.entity.StudyGoal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StudyGoalMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "studentProfile", ignore = true) // Sẽ được set ở Service
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "currentListening", constant = "0.0")
    @Mapping(target = "currentReading", constant = "0.0")
    @Mapping(target = "currentSpeaking", constant = "0.0")
    @Mapping(target = "currentWriting", constant = "0.0")
    @Mapping(target = "currentTotal", constant = "0.0")
    StudyGoal toStudyGoal(StudyGoalRequest request);

    // 💥 2. Map từ Entity sang Response (TRẢ VỀ CHO FRONTEND)
    StudyGoalResponse toStudyGoalResponse(StudyGoal studyGoal);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "studentProfile", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "currentListening", ignore = true)
    @Mapping(target = "currentReading", ignore = true)
    @Mapping(target = "currentSpeaking", ignore = true)
    @Mapping(target = "currentWriting", ignore = true)
    @Mapping(target = "currentTotal", ignore = true)
        // Chỉ update các điểm Target và loại chứng chỉ
    void updateStudyGoal(@MappingTarget StudyGoal studyGoal, StudyGoalRequest request);
}
