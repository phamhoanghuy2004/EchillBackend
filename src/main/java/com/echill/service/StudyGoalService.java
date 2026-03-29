package com.echill.service;

import com.echill.dto.request.StudyGoalRequest;
import com.echill.dto.response.StudyGoalResponse;
import com.echill.entity.StudyGoal;
import com.echill.mapper.StudyGoalMapper;
import com.echill.service.persistence.StudyGoalPersistenceService;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudyGoalService {

    StudyGoalPersistenceService studyGoalPersistenceService;
    StudyGoalMapper studyGoalMapper;


    public StudyGoalResponse createNewGoal(StudyGoalRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        StudyGoal savedGoal = studyGoalPersistenceService.createNewGoal(userId, request);
        return studyGoalMapper.toStudyGoalResponse(savedGoal);
    }

    public StudyGoalResponse updateGoal(Long goalId, StudyGoalRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        StudyGoal updatedGoal = studyGoalPersistenceService.updateGoal(goalId, userId, request);
        return studyGoalMapper.toStudyGoalResponse(updatedGoal);
    }
}