package com.echill.service;

import com.echill.dto.request.AdminCourseSearchRequest;
import com.echill.dto.request.elasticsearch.response.CourseCardResponse;
import com.echill.dto.response.CourseResponse;
import com.echill.dto.response.PageResponse;
import com.echill.entity.Course;
import com.echill.entity.enums.Status;
import com.echill.event.CourseUpdatedEvent;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.mapper.CourseMapper;
import com.echill.mapper.LessonMapper;
import com.echill.mapper.TagMapper;
import com.echill.repository.CourseRepository;
import com.echill.repository.EnrollmentRepository;
import com.echill.repository.ReviewRepository;
import com.echill.repository.specification.CourseSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminCourseService {

    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;
    ReviewRepository reviewRepository;
    CourseMapper courseMapper;
    LessonMapper lessonMapper;
    TagMapper tagMapper;
    ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<CourseCardResponse> getCourses(AdminCourseSearchRequest request) {
        Specification<Course> spec = CourseSpecification.filter(request);

        Page<Course> pageData = courseRepository.findAll(spec, request.getPageable());

        List<Long> courseIds = pageData.getContent().stream().map(Course::getId).toList();

        Map<Long, Long> studentCounts = Map.of();
        Map<Long, Long> reviewCounts = Map.of();
        Map<Long, Double> averageRatings = Map.of();

        if (!courseIds.isEmpty()) {
            studentCounts = enrollmentRepository.countEnrollmentsByCourseIds(courseIds).stream()
                    .collect(java.util.stream.Collectors.toMap(arr -> (Long) arr[0], arr -> (Long) arr[1]));

            reviewCounts = reviewRepository.countReviewsByCourseIds(courseIds).stream()
                    .collect(java.util.stream.Collectors.toMap(arr -> (Long) arr[0], arr -> (Long) arr[1]));

            averageRatings = reviewRepository.getAverageRatingsByCourseIds(courseIds).stream()
                    .collect(java.util.stream.Collectors.toMap(arr -> (Long) arr[0], arr -> (Double) arr[1]));
        }

        final Map<Long, Long> finalStudentCounts = studentCounts;
        final Map<Long, Long> finalReviewCounts = reviewCounts;
        final Map<Long, Double> finalAverageRatings = averageRatings;

        Page<CourseCardResponse> dtoPage = pageData.map(course -> {
            CourseCardResponse response = courseMapper.toCardResponse(course);
            
            // Set stats
            response.setStudentCount(finalStudentCounts.getOrDefault(course.getId(), 0L));
            response.setReviewCount(finalReviewCounts.getOrDefault(course.getId(), 0L));
            Double avg = finalAverageRatings.get(course.getId());
            response.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
            
            return response;
        });

        return PageResponse.of(dtoPage);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseDetail(Long id) {
        Course course = courseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorEnum.COURSE_NOT_FOUND));

        CourseResponse response = mapToResponse(course);

        // Fetch stats
        List<Object[]> studentCountsList = enrollmentRepository.countEnrollmentsByCourseIds(List.of(id));
        long studentCount = studentCountsList.isEmpty() ? 0L : (Long) studentCountsList.get(0)[1];
        
        List<Object[]> reviewCountsList = reviewRepository.countReviewsByCourseIds(List.of(id));
        long reviewCount = reviewCountsList.isEmpty() ? 0L : (Long) reviewCountsList.get(0)[1];

        List<Object[]> averageRatingsList = reviewRepository.getAverageRatingsByCourseIds(List.of(id));
        double avgRating = averageRatingsList.isEmpty() ? 0.0 : (Double) averageRatingsList.get(0)[1];

        response.setStudentCount(studentCount);
        response.setReviewCount(reviewCount);
        response.setAverageRating(Math.round(avgRating * 10.0) / 10.0);

        return response;
    }

    @Transactional
    public CourseResponse updateCourseStatus(Long id, Status status) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorEnum.COURSE_NOT_FOUND));

        course.setStatus(status);
        Course savedCourse = courseRepository.save(course);

        eventPublisher.publishEvent(new CourseUpdatedEvent(savedCourse.getId()));

        return getCourseDetail(savedCourse.getId());
    }

    private CourseResponse mapToResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .price(course.getPrice())
                .originalPrice(course.getOriginalPrice())
                .imageUrl(course.getImageUrl())
                .level(course.getLevel())
                .status(course.getStatus())
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
                        .map(lessonMapper::toLessonResponse)
                        .toList())
                .tags(course.getTags() == null || course.getTags().isEmpty()
                        ? List.of()
                        : course.getTags().stream()
                        .map(tagMapper::toResponse)
                        .toList())
                .build();
    }
}
