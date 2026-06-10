package com.echill.service;

import com.echill.dto.response.teacher.CourseDetailReportResponse;
import com.echill.dto.response.teacher.RevenueChartResponse;
import com.echill.dto.response.teacher.TeacherSummaryResponse;
import com.echill.dto.response.teacher.TopCourseResponse;
import com.echill.entity.Course;
import com.echill.repository.CourseRepository;
import com.echill.repository.EnrollmentRepository;
import com.echill.repository.ReviewRepository;
import com.echill.repository.TransactionRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeacherAnalyticsService {

    CourseRepository courseRepository;
    TransactionRepository transactionRepository;
    EnrollmentRepository enrollmentRepository;
    ReviewRepository reviewRepository;

    public TeacherSummaryResponse getSummary(Long courseId) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        
        long totalCourses = courseRepository.countByTeacherId(teacherId);
        
        BigDecimal totalRevenue;
        long totalStudents;
        Double avgRating;

        if (courseId == null) {
            totalRevenue = transactionRepository.getTotalRevenueByTeacherId(teacherId);
            totalStudents = transactionRepository.countSalesByTeacherId(teacherId);
            avgRating = reviewRepository.getAverageRatingByTeacherId(teacherId);
        } else {
            totalRevenue = transactionRepository.getTotalRevenueByTeacherIdAndCourseId(teacherId, courseId);
            totalStudents = transactionRepository.countSalesByTeacherIdAndCourseId(teacherId, courseId);
            avgRating = reviewRepository.getAverageRatingByTeacherIdAndCourseId(teacherId, courseId);
        }

        return TeacherSummaryResponse.builder()
                .totalCourses(totalCourses)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalStudents(totalStudents)
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .build();
    }

    public List<RevenueChartResponse> getRevenueChart(Long courseId, String period, Integer year) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        List<Object[]> results = transactionRepository.getRevenueByPeriod(teacherId, courseId, period, year);
        
        return results.stream()
                .map(row -> RevenueChartResponse.builder()
                        .label((String) row[0])
                        .value((BigDecimal) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    public List<TopCourseResponse> getTopSellingCourses() {
        Long teacherId = SecurityUtils.getCurrentUserId();

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Instant fromDate = today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toDate = today.withDayOfMonth(today.lengthOfMonth()).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> salesStats = transactionRepository.findTopSellingCoursesByTeacherIdAndDateRange(teacherId, fromDate, toDate);
        
        // Lấy danh sách ID để fetch thêm thông tin nếu cần, hoặc tính toán revenue trực tiếp
        // Để tối ưu, ta có thể lấy revenue theo từng course
        return salesStats.stream()
                .limit(5) // Lấy top 5
                .map(row -> {
                    Long cId = (Long) row[0];
                    String cName = (String) row[1];
                    Long sCount = (Long) row[2];
                    
                    BigDecimal revenue = transactionRepository.getRevenueByCourseIdAndDateRange(cId, fromDate, toDate);
                    Double avgRating = reviewRepository.getAverageRatingByCourseId(cId);
                    
                    return TopCourseResponse.builder()
                            .courseId(cId)
                            .courseName(cName)
                            .studentCount(sCount)
                            .revenue(revenue != null ? revenue : BigDecimal.ZERO)
                            .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getMyCoursesBasicInfo() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        List<Object[]> courses = courseRepository.findBasicInfoByTeacherId(teacherId);
        
        return courses.stream()
                .map(row -> Map.of(
                        "id", row[0].toString(),
                        "name", row[1]
                ))
                .collect(Collectors.toList());
    }

    public List<CourseDetailReportResponse> getTopCoursesDetailReport(Instant fromDate, Instant toDate, String sortBy) {
        Long teacherId = SecurityUtils.getCurrentUserId();

        // Lấy danh sách khóa học của giảng viên (chỉ lấy thông tin cơ bản, không fetch lessons để tránh load chậm)
        List<Course> courses = courseRepository.findAllByTeacherIdWithBasicDetails(teacherId);
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        if (courseIds.isEmpty()) {
            return List.of();
        }

        // Lấy doanh thu và số lượng bán theo từng khóa học trong khoảng thời gian (Batch)
        List<Object[]> revenueAndSalesRows = transactionRepository.getRevenueAndSalesPerCourseByDateRange(teacherId, fromDate, toDate);
        Map<Long, BigDecimal> revenueMap = revenueAndSalesRows.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO
                ));

        Map<Long, Long> salesMap = revenueAndSalesRows.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> row[2] != null ? ((Number) row[2]).longValue() : 0L
                ));



        // Lấy điểm đánh giá trung bình (Batch, all time)
        List<Object[]> ratingRows = reviewRepository.getAverageRatingsByCourseIds(courseIds);
        Map<Long, Double> ratingMap = ratingRows.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> row[1] != null ? (Double) row[1] : 0.0
                ));

        AtomicInteger rank = new AtomicInteger(1);

        return courses.stream()
                .map(course -> {
                    BigDecimal revenue = revenueMap.getOrDefault(course.getId(), BigDecimal.ZERO);
                    Long sales = salesMap.getOrDefault(course.getId(), 0L);
                    Double avgRating = ratingMap.getOrDefault(course.getId(), 0.0);

                    return CourseDetailReportResponse.builder()
                            .courseId(course.getId())
                            .courseName(course.getName())
                            .teacherName(course.getTeacher().getFullName())
                            .teacherAvatar(course.getTeacher().getAvatarUrl())
                            .revenue(revenue)
                            .salesCount(sales)
                            .averageRating(Math.round(avgRating * 10.0) / 10.0)
                            .build();
                })
                .sorted((a, b) -> {
                    if ("SALES".equalsIgnoreCase(sortBy)) {
                        int cmp = Long.compare(b.getSalesCount(), a.getSalesCount());
                        if (cmp != 0) return cmp;
                        return b.getRevenue().compareTo(a.getRevenue());
                    } else {
                        // Mặc định là REVENUE
                        int cmp = b.getRevenue().compareTo(a.getRevenue());
                        if (cmp != 0) return cmp;
                        return Long.compare(b.getSalesCount(), a.getSalesCount());
                    }
                })
                .peek(r -> r.setRank(rank.getAndIncrement()))
                .collect(Collectors.toList());
    }
}
