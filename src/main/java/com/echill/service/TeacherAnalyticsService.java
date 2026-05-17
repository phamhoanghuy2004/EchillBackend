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
            totalStudents = enrollmentRepository.countStudentsByTeacherId(teacherId);
            avgRating = reviewRepository.getAverageRatingByTeacherId(teacherId);
        } else {
            totalRevenue = transactionRepository.getTotalRevenueByTeacherIdAndCourseId(teacherId, courseId);
            totalStudents = enrollmentRepository.countStudentsByTeacherIdAndCourseId(teacherId, courseId);
            avgRating = reviewRepository.getAverageRatingByTeacherIdAndCourseId(teacherId, courseId);
        }

        return TeacherSummaryResponse.builder()
                .totalCourses(totalCourses)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalStudents(totalStudents)
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .build();
    }

    public List<RevenueChartResponse> getRevenueChart(Long courseId, String period) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        List<Object[]> results = transactionRepository.getRevenueByPeriod(teacherId, courseId, period);
        
        return results.stream()
                .map(row -> RevenueChartResponse.builder()
                        .label((String) row[0])
                        .value((BigDecimal) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    public List<TopCourseResponse> getTopSellingCourses() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        List<Object[]> enrollStats = enrollmentRepository.findTopSellingCoursesByTeacherId(teacherId);
        
        // Lấy danh sách ID để fetch thêm thông tin nếu cần, hoặc tính toán revenue trực tiếp
        // Để tối ưu, ta có thể lấy revenue theo từng course
        return enrollStats.stream()
                .limit(5) // Lấy top 5
                .map(row -> {
                    Long cId = (Long) row[0];
                    String cName = (String) row[1];
                    Long sCount = (Long) row[2];
                    
                    BigDecimal revenue = transactionRepository.getTotalRevenueByTeacherIdAndCourseId(teacherId, cId);
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
                        "id", row[0],
                        "name", row[1]
                ))
                .collect(Collectors.toList());
    }

    public List<CourseDetailReportResponse> getTopCoursesDetailReport(Instant fromDate, Instant toDate) {
        Long teacherId = SecurityUtils.getCurrentUserId();

        // Lấy danh sách khóa học của giảng viên kèm thông tin giảng viên
        List<Course> courses = courseRepository.findAllByTeacherIdWithDetails(teacherId);

        // Lấy doanh thu theo từng khóa học trong khoảng thời gian
        List<Object[]> revenueRows = transactionRepository.getRevenuePerCourseByDateRange(teacherId, fromDate, toDate);
        Map<Long, BigDecimal> revenueMap = revenueRows.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO
                ));

        AtomicInteger rank = new AtomicInteger(1);

        return courses.stream()
                .map(course -> {
                    BigDecimal revenue = transactionRepository.getRevenueByCourseIdAndDateRange(course.getId(), fromDate, toDate);
                    Long sales = transactionRepository.getSalesCountByCourseIdAndDateRange(course.getId(), fromDate, toDate);
                    Double avgRating = reviewRepository.getAverageRatingByCourseId(course.getId());

                    return CourseDetailReportResponse.builder()
                            .courseId(course.getId())
                            .courseName(course.getName())
                            .teacherName(course.getTeacher().getFullName())
                            .teacherAvatar(course.getTeacher().getAvatarUrl())
                            .revenue(revenue != null ? revenue : BigDecimal.ZERO)
                            .salesCount(sales != null ? sales : 0L)
                            .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                            .build();
                })
                .sorted(Comparator.comparing(CourseDetailReportResponse::getRevenue).reversed())
                .peek(r -> r.setRank(rank.getAndIncrement()))
                .collect(Collectors.toList());
    }
}
