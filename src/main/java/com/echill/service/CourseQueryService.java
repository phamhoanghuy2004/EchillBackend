package com.echill.service;

import com.echill.constant.CacheNames;
import com.echill.dto.request.elasticsearch.response.CourseCardResponse;
import com.echill.dto.response.guest.CourseDetailResponse;
import com.echill.entity.Course;
import com.echill.exception.AppException;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.CourseMapper;
import com.echill.repository.CourseRepository;
import com.echill.repository.EnrollmentRepository;
import com.echill.repository.ReviewRepository;
import org.springframework.cache.annotation.Cacheable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseQueryService {
    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;
    ReviewRepository reviewRepository;
    CourseMapper courseMapper;

    @Cacheable(cacheNames = CacheNames.COURSE_DETAIL, key = "'course:' + #id", sync = true)
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long id) {
        log.info("⚡ CHẠY VÀO DB ĐỂ LẤY CHI TIẾT KHÓA HỌC ID: {} (CACHE MISS)", id);
        Course course = courseRepository.findActiveCourseWithFullDetails(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.COURSE_NOT_FOUND));

        CourseDetailResponse response = courseMapper.toDetailResponse(course);
        response = response.toBuilder()
                .studentCount(enrollmentRepository.countByCourseId(id))
                .reviewCount(reviewRepository.countByCourseId(id))
                .averageRating(getFormattedAvgRating(id))
                .build();

        hideSensitiveData(response);
        return response;
    }

    @Transactional(readOnly = true)
    public java.util.List<CourseCardResponse> getAllCourses() {
        log.info("⚡ CHẠY VÀO DB ĐỂ LẤY TẤT CẢ KHÓA HỌC (CACHE MISS)");
        List<Course> courses = courseRepository.findAllActiveCoursesSortedByNewest();
        List<Long> courseIds = courses.stream().map(Course::getId).toList();

        Map<Long, Long> studentCounts = enrollmentRepository.countEnrollmentsByCourseIds(courseIds).stream()
                .collect(java.util.stream.Collectors.toMap(arr -> (Long) arr[0], arr -> (Long) arr[1]));

        Map<Long, Long> reviewCounts = reviewRepository.countReviewsByCourseIds(courseIds).stream()
                .collect(java.util.stream.Collectors.toMap(arr -> (Long) arr[0], arr -> (Long) arr[1]));

        Map<Long, Double> averageRatings = reviewRepository.getAverageRatingsByCourseIds(courseIds).stream()
                .collect(java.util.stream.Collectors.toMap(arr -> (Long) arr[0], arr -> (Double) arr[1]));

        return courses.stream()
                .map(course -> {
                    CourseCardResponse card = courseMapper.toCardResponse(course);
                    card.setStudentCount(studentCounts.getOrDefault(course.getId(), 0L));
                    card.setReviewCount(reviewCounts.getOrDefault(course.getId(), 0L));
                    Double avg = averageRatings.get(course.getId());
                    card.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
                    return card;
                })
                .toList();
    }

    private Double getFormattedAvgRating(Long courseId) {
        Double avg = reviewRepository.getAverageRatingByCourseId(courseId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    private void hideSensitiveData(CourseDetailResponse response) {
        if (response.getLessons() != null){
            response.getLessons().forEach(lesson -> {
                if (!Boolean.TRUE.equals(lesson.getIsPreview())){
                    lesson.setPreviewVideoUrl(null);
                    if (lesson.getDocuments() != null){
                        lesson.getDocuments().forEach(document -> {
                            document.setFileUrl(null);
                        });
                    }
                }
            });
        }
    }
}
