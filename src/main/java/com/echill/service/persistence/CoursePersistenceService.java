package com.echill.service.persistence;

import com.echill.dto.request.CourseRequest;
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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CoursePersistenceService {

    CourseRepository courseRepository;
    CategoryRepository categoryRepository;
    UserRepository userRepository;

    // 💥 Mở Transaction ở đây, thao tác thuần Database, cực kỳ nhanh!
    @Transactional
    public Course saveNewCourse(Long teacherId, CourseRequest request, String uploadedImageUrl) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(TeacherErrorEnum.CATEGORY_NOT_FOUND));

        Course course = Course.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .imageUrl(uploadedImageUrl) // Nhận link URL đã upload
                .level(request.getLevel())
                .category(category)
                .teacher(teacher)
                .status(Status.ACTIVE)
                .build();

        return courseRepository.save(course);
    }

    // Gắn readOnly = true để Hibernate tối ưu bộ nhớ, không sinh ra dirty checking
    @Transactional(readOnly = true)
    public List<Course> getAllCoursesByTeacherId(Long teacherId) {
        return courseRepository.findAllByTeacherIdWithDetails(teacherId);
    }

    @Transactional(readOnly = true)
    public Course getCourseById(Long courseId) {
        return courseRepository.findByIdWithDetails(courseId)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.COURSE_NOT_FOUND));
    }
}