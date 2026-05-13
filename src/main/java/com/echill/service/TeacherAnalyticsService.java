package com.echill.service;

import com.echill.dto.response.teacher.RevenueChartResponse;
import com.echill.dto.response.teacher.TeacherSummaryResponse;
import com.echill.dto.response.teacher.TopCourseResponse;
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
import java.util.List;
import java.util.Map;
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
}
