package com.echill.service;

import com.echill.constant.CloudinaryFolder;
import com.echill.dto.request.CourseRequest;
import com.echill.dto.response.CourseResponse;
import com.echill.dto.response.LessonResponse;
import com.echill.entity.Category;
import com.echill.entity.Course;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.repository.CategoryRepository;
import com.echill.repository.CourseRepository;
import com.echill.repository.UserRepository;
import com.echill.service.persistence.CoursePersistenceService;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseService {

    CloudinaryService cloudinaryService;
    CoursePersistenceService coursePersistenceService;
    UserRepository userRepository;
    CategoryRepository categoryRepository;
    CourseRepository courseRepository;

    public CourseResponse createCourse(CourseRequest request, MultipartFile file) {
        Long teacherId = SecurityUtils.getCurrentUserId();

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(TeacherErrorEnum.CATEGORY_NOT_FOUND));

        Map<String, String> uploadResult = null;
        if (file != null && !file.isEmpty()) {
            uploadResult = cloudinaryService.uploadImage(file, CloudinaryFolder.COURSE_IMAGE);
        }

        String url = (uploadResult != null) ? uploadResult.get("url") : null;
        String pId = (uploadResult != null) ? uploadResult.get("publicId") : null;

        Course course = coursePersistenceService.saveNewCourse(teacher, category, request, url, pId);
        return mapToResponse(course);

    }

    public CourseResponse updateCourse(Long id, CourseRequest request, MultipartFile file) {
        Course existingCourse = courseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.COURSE_NOT_FOUND));

        SecurityUtils.validateOwnership(existingCourse.getTeacher().getId());

        String newImageUrl = null;
        String newImagePublicId = null;

        if (file != null && !file.isEmpty()) {
            Map<String, String> uploadResult = cloudinaryService.uploadImage(file, CloudinaryFolder.COURSE_IMAGE);
            newImageUrl = uploadResult.get("url");
            newImagePublicId = uploadResult.get("publicId");
        }

        Course updatedCourse = coursePersistenceService.updateCourseData(existingCourse, request, newImageUrl, newImagePublicId);

        return mapToResponse(updatedCourse);
    }

    public List<CourseResponse> getAllCoursesByTeacher() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return courseRepository.findAllByTeacherIdWithDetails(teacherId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CourseResponse getCourseById(Long id) {
        Course course= courseRepository.findByIdWithDetails(id)
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
                .lessons(course.getLessons() == null || course.getLessons().isEmpty()
                        ? List.of()
                        : course.getLessons().stream()
                        .map(l -> LessonResponse.builder()
                                .id(l.getId())
                                .title(l.getTitle())
                                .content(l.getContent())
                                .displayOrder(l.getDisplayOrder())
                                .isPreview(l.getIsPreview())
                                .publicVideoId(l.getPublicVideoId())
                                .rawUrl(l.getRawUrl())
                                .hlsUrl(l.getHlsUrl())
                                .videoStatus(l.getVideoStatus())
                                .durationSeconds(l.getDurationSeconds())
                                .build())
                        .toList())
                .build();
    }
}