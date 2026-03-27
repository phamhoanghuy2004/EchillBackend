package com.echill.service;

import com.echill.constant.CloudinaryFolder;
import com.echill.dto.request.CourseRequest;
import com.echill.dto.response.CourseResponse;
import com.echill.dto.response.LessonResponse;
import com.echill.mapper.LessonMapper;
import com.echill.entity.Category;
import com.echill.entity.Course;
import com.echill.entity.User;
import com.echill.entity.enums.Status;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.repository.CategoryRepository;
import com.echill.repository.CourseRepository;
import com.echill.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final LessonMapper lessonMapper;

    @Transactional
    public CourseResponse createCourse(CourseRequest request, MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User teacher = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(TeacherErrorEnum.CATEGORY_NOT_FOUND));

        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            imageUrl = cloudinaryService.uploadImage(file, CloudinaryFolder.COURSE_IMAGE);
        }

        Course course = Course.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .imageUrl(imageUrl)
                .level(request.getLevel())
                .category(category)
                .teacher(teacher)
                .status(Status.ACTIVE)
                .build();

        course = courseRepository.save(course);
        return mapToResponse(course);
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request, MultipartFile file) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.COURSE_NOT_FOUND));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(TeacherErrorEnum.CATEGORY_NOT_FOUND));

        if (file != null && !file.isEmpty()) {
            // Delete old image if exists
            if (course.getImageUrl() != null) {
                cloudinaryService.deleteImage(course.getImageUrl());
            }
            String imageUrl = cloudinaryService.uploadImage(file, CloudinaryFolder.COURSE_IMAGE);
            course.setImageUrl(imageUrl);
        }

        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setPrice(request.getPrice());
        course.setOriginalPrice(request.getOriginalPrice());
        course.setLevel(request.getLevel());
        course.setCategory(category);

        course = courseRepository.save(course);
        return mapToResponse(course);
    }

    public List<CourseResponse> getAllCoursesByTeacher() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return courseRepository.findByTeacherUsername(username).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findByIdWithLessons(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.COURSE_NOT_FOUND));
        return mapToResponse(course);
    }

    private CourseResponse mapToResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId().toString())
                .name(course.getName())
                .description(course.getDescription())
                .price(course.getPrice())
                .originalPrice(course.getOriginalPrice())
                .imageUrl(course.getImageUrl())
                .level(course.getLevel())
                .categoryId(course.getCategory().getId())
                .categoryName(course.getCategory().getName())
                .teacherName(course.getTeacher().getFullName())
                .createdAt(course.getCreatedAt() != null ? 
                    course.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                .lessons(course.getLessons() != null ? 
                        course.getLessons().stream()
                                .map(lessonMapper::toLessonResponse)
                                .collect(Collectors.toList()) : null)
                .build();
    }
}
