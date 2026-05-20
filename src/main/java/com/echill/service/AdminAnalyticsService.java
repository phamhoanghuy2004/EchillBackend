package com.echill.service;

import com.echill.dto.response.admin.AdminFiltersResponse;
import com.echill.dto.response.admin.AdminSummaryResponse;
import com.echill.dto.response.admin.CourseRankingResponse;
import com.echill.dto.response.admin.TeacherRankingResponse;
import com.echill.dto.response.teacher.RevenueChartResponse;
import com.echill.repository.CourseRepository;
import com.echill.repository.ReviewRepository;
import com.echill.repository.TransactionRepository;
import com.echill.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminAnalyticsService {

    TransactionRepository transactionRepository;
    UserRepository userRepository;
    CourseRepository courseRepository;
    ReviewRepository reviewRepository;

    public AdminSummaryResponse getSummary() {
        BigDecimal totalRevenue = transactionRepository.getTotalSystemRevenue();
        long totalCourses = courseRepository.count();
        long totalStudents = userRepository.countStudents();
        long totalTeachers = userRepository.countTeachers();

        return AdminSummaryResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalCourses(totalCourses)
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .build();
    }

    public List<RevenueChartResponse> getRevenueChart(Instant fromDate, Instant toDate, Long teacherId, Long courseId) {
        List<Object[]> results = transactionRepository.getAdminRevenueChartData(fromDate, toDate, teacherId, courseId);

        return results.stream()
                .map(row -> RevenueChartResponse.builder()
                        .label((String) row[0])
                        .value(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());
    }

    public List<TeacherRankingResponse> getTeacherRankings() {
        List<Object[]> results = transactionRepository.getTeacherRankings();
        AtomicInteger rank = new AtomicInteger(1);

        return results.stream()
                .map(row -> TeacherRankingResponse.builder()
                        .rank(rank.getAndIncrement())
                        .teacherId(((Number) row[0]).longValue())
                        .teacherName((String) row[1])
                        .teacherAvatar((String) row[2])
                        .revenue(row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO)
                        .salesCount(row[4] != null ? ((Number) row[4]).longValue() : 0L)
                        .build())
                .collect(Collectors.toList());
    }

    public List<CourseRankingResponse> getCourseRankings() {
        List<Object[]> results = transactionRepository.getCourseRankings();
        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> courseIds = results.stream()
                .map(row -> ((Number) row[0]).longValue())
                .collect(Collectors.toList());

        Map<Long, Double> ratingsMap = new HashMap<>();
        List<Object[]> ratings = reviewRepository.getAverageRatingsByCourseIds(courseIds);
        if (ratings != null) {
            ratingsMap = ratings.stream().collect(Collectors.toMap(
                    row -> ((Number) row[0]).longValue(),
                    row -> row[1] != null ? Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0 : 0.0,
                    (r1, r2) -> r1
            ));
        }

        final Map<Long, Double> finalRatingsMap = ratingsMap;
        AtomicInteger rank = new AtomicInteger(1);

        return results.stream()
                .map(row -> {
                    Long courseId = ((Number) row[0]).longValue();
                    return CourseRankingResponse.builder()
                            .rank(rank.getAndIncrement())
                            .courseId(courseId)
                            .courseName((String) row[1])
                            .teacherName((String) row[2])
                            .teacherAvatar((String) row[3])
                            .revenue(row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO)
                            .salesCount(row[5] != null ? ((Number) row[5]).longValue() : 0L)
                            .averageRating(finalRatingsMap.getOrDefault(courseId, 0.0))
                            .build();
                })
                .collect(Collectors.toList());
    }

    public AdminFiltersResponse getFilters() {
        List<Object[]> teachersRaw = userRepository.findAllTeachersBasicInfo();
        List<Map<String, Object>> teachers = teachersRaw.stream()
                .map(row -> Map.of(
                        "id", String.valueOf(row[0]),
                        "name", row[1]
                ))
                .collect(Collectors.toList());

        List<Object[]> coursesRaw = courseRepository.findAllCoursesBasicInfo();
        List<Map<String, Object>> courses = coursesRaw.stream()
                .map(row -> Map.of(
                        "id", String.valueOf(row[0]),
                        "name", row[1],
                        "teacherId", String.valueOf(row[2])
                ))
                .collect(Collectors.toList());

        return AdminFiltersResponse.builder()
                .teachers(teachers)
                .courses(courses)
                .build();
    }
}
