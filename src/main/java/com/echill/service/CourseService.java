package com.echill.service;

import com.echill.constant.CacheNames;
import com.echill.constant.CloudinaryFolder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import com.echill.dto.request.CourseRequest;
import com.echill.dto.response.CourseResponse;
import com.echill.dto.response.LessonResponse;
import com.echill.entity.Category;
import com.echill.entity.Course;
import com.echill.entity.Tag;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.TagMapper;
import com.echill.repository.*;
import com.echill.service.persistence.CoursePersistenceService;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZoneId;
import java.util.ArrayList;
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
    TagRepository tagRepository;
    EnrollmentRepository enrollmentRepository;
    ReviewRepository reviewRepository;
    TagMapper tagMapper;

    public CourseResponse createCourse(CourseRequest request, MultipartFile file) {
        Long teacherId = SecurityUtils.getCurrentUserId();

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(TeacherErrorEnum.CATEGORY_NOT_FOUND));

        List<Tag> validTags = new ArrayList<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            validTags = tagRepository.findAllById(request.getTagIds());

            if (validTags.size() != request.getTagIds().size()) {
                throw new AppException(TeacherErrorEnum.TAG_NOT_FOUND);
            }
        }

        Map<String, String> uploadResult = null;
        if (file != null && !file.isEmpty()) {
            uploadResult = cloudinaryService.uploadImage(file, CloudinaryFolder.COURSE_IMAGE);
        }

        String url = (uploadResult != null) ? uploadResult.get("url") : null;
        String pId = (uploadResult != null) ? uploadResult.get("publicId") : null;

        Course course = coursePersistenceService.saveNewCourse(teacher, category, validTags, request, url, pId);

        return mapToResponse(course);

    }


    public CourseResponse updateCourse(Long id, CourseRequest request, MultipartFile file) {
        Course existingCourse = courseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.COURSE_NOT_FOUND));

        SecurityUtils.validateOwnership(existingCourse.getTeacher().getId());

        List<Tag> validTags = new ArrayList<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            validTags = tagRepository.findAllById(request.getTagIds());

            if (validTags.size() != request.getTagIds().size()) {
                throw new AppException(TeacherErrorEnum.TAG_NOT_FOUND);
            }
        }

        String newImageUrl = null;
        String newImagePublicId = null;

        if (file != null && !file.isEmpty()) {
            Map<String, String> uploadResult = cloudinaryService.uploadImage(file, CloudinaryFolder.COURSE_IMAGE);
            newImageUrl = uploadResult.get("url");
            newImagePublicId = uploadResult.get("publicId");
        }

        Course updatedCourse = coursePersistenceService.updateCourseData(existingCourse, request, validTags, newImageUrl, newImagePublicId);

        return mapToResponse(updatedCourse);
    }

    public List<CourseResponse> getAllCoursesByTeacher() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        List<Course> courses = courseRepository.findAllByTeacherIdWithDetails(teacherId);
        List<Long> courseIds = courses.stream().map(Course::getId).toList();

        Map<Long, Long> studentCounts = enrollmentRepository.countEnrollmentsByCourseIds(courseIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> (Long) arr[1]
                ));

        Map<Long, Long> reviewCounts = reviewRepository.countReviewsByCourseIds(courseIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> (Long) arr[1]
                ));

        Map<Long, Double> averageRatings = reviewRepository.getAverageRatingsByCourseIds(courseIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> (Double) arr[1]
                ));

        return courses.stream()
                .map(course -> {
                    CourseResponse response = mapToResponse(course);
                    response.setStudentCount(studentCounts.getOrDefault(course.getId(), 0L));
                    response.setReviewCount(reviewCounts.getOrDefault(course.getId(), 0L));
                    Double avg = averageRatings.get(course.getId());
                    response.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
                    return response;
                })
                .toList();
    }

    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.COURSE_NOT_FOUND));
        
        CourseResponse response = mapToResponse(course);
        response.setStudentCount(enrollmentRepository.countByCourseId(id));
        response.setReviewCount(reviewRepository.countByCourseId(id));
        Double avg = reviewRepository.getAverageRatingByCourseId(id);
        response.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        
        return response;
    }

    @Cacheable(cacheNames = "topCourses_v2", key = "'top_6_purchased'")
    public List<CourseResponse> getTop6PurchasedCourses() {
        log.info("⚡ FETCHING TOP 6 PURCHASED COURSES FROM DB (REFRESHED)");
        List<Course> courses = courseRepository.findTop6MostPurchasedCourses(org.springframework.data.domain.PageRequest.of(0, 6));
        List<Long> courseIds = courses.stream().map(Course::getId).toList();

        if (courseIds.isEmpty()) return List.of();

        // Lấy các chỉ số thống kê
        Map<Long, Long> studentCounts = enrollmentRepository.countEnrollmentsByCourseIds(courseIds).stream()
                .collect(java.util.stream.Collectors.toMap(arr -> (Long) arr[0], arr -> (Long) arr[1]));

        Map<Long, Long> reviewCounts = reviewRepository.countReviewsByCourseIds(courseIds).stream()
                .collect(java.util.stream.Collectors.toMap(arr -> (Long) arr[0], arr -> (Long) arr[1]));

        Map<Long, Double> averageRatings = reviewRepository.getAverageRatingsByCourseIds(courseIds).stream()
                .collect(java.util.stream.Collectors.toMap(arr -> (Long) arr[0], arr -> (Double) arr[1]));

        return courses.stream()
                .map(course -> {
                    CourseResponse response = mapToResponse(course);
                    response.setStudentCount(studentCounts.getOrDefault(course.getId(), 0L));
                    response.setReviewCount(reviewCounts.getOrDefault(course.getId(), 0L));
                    Double avg = averageRatings.get(course.getId());
                    response.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
                    return response;
                })
                .toList();
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
                .teacherId(course.getTeacher().getId())
                .teacherAvatarUrl(course.getTeacher().getAvatarUrl() != null ? 
                        course.getTeacher().getAvatarUrl() : 
                        "https://ui-avatars.com/api/?name=" + java.net.URLEncoder.encode(course.getTeacher().getFullName(), java.nio.charset.StandardCharsets.UTF_8) + "&background=random")
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

                .tags(course.getTags() == null || course.getTags().isEmpty()
                        ? List.of()
                        : course.getTags().stream()
                        .map(tagMapper::toResponse)
                        .toList())

                .build();
    }


}