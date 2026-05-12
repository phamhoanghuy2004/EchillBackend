package com.echill.service;

import com.echill.dto.request.ReviewRequest;
import com.echill.dto.response.ReviewResponse;
import com.echill.entity.Course;
import com.echill.entity.Review;
import com.echill.entity.User;
import com.echill.constant.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.CourseRepository;
import com.echill.repository.EnrollmentRepository;
import com.echill.repository.ReviewRepository;
import com.echill.repository.UserRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewService {

    ReviewRepository reviewRepository;
    CourseRepository courseRepository;
    UserRepository userRepository;
    EnrollmentRepository enrollmentRepository;

    @Transactional
    @CacheEvict(cacheNames = "featuredReviews", allEntries = true)
    public ReviewResponse createOrUpdateReview(ReviewRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        // 1. Kiểm tra enrollment
        boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(userId, request.getCourseId());
        if (!isEnrolled) {
            throw new AppException(ErrorEnum.NOT_ENROLLED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));
        
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new AppException(ErrorEnum.COURSE_NOT_FOUND));

        // 2. Tìm review cũ (nếu có)
        Review review = reviewRepository.findByUserAndCourse(user, course)
                .orElse(Review.builder()
                        .user(user)
                        .course(course)
                        .build());

        // 3. Cập nhật thông tin
        review.updateReview(request.getRating(), request.getContent());
        
        Review savedReview = reviewRepository.save(review);
        
        return mapToResponse(savedReview);
    }

    public ReviewResponse getMyReviewByCourse(Long courseId) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(ErrorEnum.COURSE_NOT_FOUND));

        Review review = reviewRepository.findByUserAndCourse(user, course)
                .orElseThrow(() -> new AppException(ErrorEnum.REVIEW_NOT_FOUND));

        return mapToResponse(review);
    }

    public List<ReviewResponse> getReviewsByCourse(Long courseId) {
        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<ReviewResponse> getPaginatedReviewsByCourse(Long courseId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId, pageable)
                .map(this::mapToResponse);
    }
    // Tạm thời bỏ cache để debug dữ liệu thực
    public List<ReviewResponse> getFeaturedReviews() {
        List<Review> reviews = reviewRepository.findFeaturedReviews(PageRequest.of(0, 10));
        return reviews.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .content(review.getContent())
                .courseId(review.getCourse().getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .userAvatar(review.getUser().getAvatarUrl())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
