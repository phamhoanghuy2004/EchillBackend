package com.echill.service;

import com.echill.dto.response.TestResponse;
import com.echill.entity.Question;
import com.echill.entity.QuestionGroup;
import com.echill.entity.Test;
import com.echill.exception.AppException;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.TestMapper;
import com.echill.repository.QuestionGroupRepository;
import com.echill.repository.QuestionRepository;
import com.echill.repository.TestRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminTestService {

    CloudinaryService cloudinaryService;
    QuestionRepository questionRepository;
    QuestionGroupRepository questionGroupRepository;
    TestRepository testRepository;
    TestMapper testMapper;

    public TestResponse getTestById(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.TEST_NOT_FOUND));
        return testMapper.toResponse(test);
    }

    @Transactional
    public String uploadQuestionAudio(Long id, MultipartFile file) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.QUESTION_NOT_FOUND));

        Map<String, String> uploadResult = cloudinaryService.uploadAudio(file, "toeic/audio/questions/" + id);
        question.setAudioUrl(uploadResult.get("url"));
        question.setAudioPublicId(uploadResult.get("public_id"));
        questionRepository.save(question);

        return uploadResult.get("url");
    }

    @Transactional
    public String uploadQuestionImage(Long id, MultipartFile file) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.QUESTION_NOT_FOUND));

        Map<String, String> uploadResult = cloudinaryService.uploadImage(file, "toeic/image/questions/" + id);
        question.setImageUrl(uploadResult.get("url"));
        question.setImagePublicId(uploadResult.get("public_id"));
        questionRepository.save(question);

        return uploadResult.get("url");
    }

    @Transactional
    public String uploadGroupAudio(Long id, MultipartFile file) {
        QuestionGroup group = questionGroupRepository.findById(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.QUESTION_NOT_FOUND));

        Map<String, String> uploadResult = cloudinaryService.uploadAudio(file, "toeic/audio/groups/" + id);
        group.setSharedAudioUrl(uploadResult.get("url"));
        group.setSharedAudioPublicId(uploadResult.get("public_id"));
        questionGroupRepository.save(group);

        return uploadResult.get("url");
    }

    @Transactional
    public String uploadGroupImage(Long id, MultipartFile file) {
        QuestionGroup group = questionGroupRepository.findById(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.QUESTION_NOT_FOUND));

        Map<String, String> uploadResult = cloudinaryService.uploadImage(file, "toeic/image/groups/" + id);
        group.setSharedImageUrl(uploadResult.get("url"));
        group.setSharedImagePublicId(uploadResult.get("public_id"));
        questionGroupRepository.save(group);

        return uploadResult.get("url");
    }
}
