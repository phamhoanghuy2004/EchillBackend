package com.echill.service;

import com.echill.dto.request.LessonCreationRequest;
import com.echill.dto.response.LessonResponse;
import com.echill.entity.Course;
import com.echill.entity.Lesson;
import com.echill.exception.AppException;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.LessonMapper;
import com.echill.repository.CourseRepository;
import com.echill.repository.LessonRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonService {
    LessonRepository lessonRepository;
    LessonMapper lessonMapper;
    CourseRepository courseRepository;

    @Transactional
    public LessonResponse createLesson(LessonCreationRequest request) {
        Course course = courseRepository.findByIdWithTeacher(request.getCourseId())
                .orElseThrow(() -> new AppException(TeacherErrorEnum.COURSE_NOT_FOUND));

        SecurityUtils.validateOwnership(course.getTeacher().getId());

        Lesson lesson = Lesson.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .displayOrder(request.getDisplayOrder())
                .isPreview(request.getIsPreview())
                .course(course)
                .build();

        lessonRepository.save(lesson);
        log.info("Đã tạo mới khung bài học (Text) thành công, Lesson ID: {}", lesson.getId());

        return lessonMapper.toLessonResponse(lesson);
    }
}
