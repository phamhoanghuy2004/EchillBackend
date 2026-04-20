package com.echill.service.persistence;

import com.echill.dto.request.CourseRequest;
import com.echill.entity.Category;
import com.echill.entity.Course;
import com.echill.entity.Tag;
import com.echill.entity.User;
import com.echill.entity.enums.Status;
import com.echill.event.CourseCreatedEvent;
import com.echill.event.CourseUpdatedEvent;
import com.echill.repository.CategoryRepository;
import com.echill.repository.CourseRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CoursePersistenceService {

    CourseRepository courseRepository;
    CategoryRepository categoryRepository;
    ApplicationEventPublisher eventPublisher;

    @Transactional
    public Course saveNewCourse(User teacher, Category category, List<Tag> tags, CourseRequest request, String uploadedImageUrl, String publicImageId) {
        Course course = Course.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .imageUrl(uploadedImageUrl)
                .imagePublicId(publicImageId)
                .level(request.getLevel())
                .category(category)
                .teacher(teacher)
                .status(Status.ACTIVE)
                .build();

        if (tags != null && !tags.isEmpty()) {
            tags.forEach(course::addTag);
        }

        Course savedCourse = courseRepository.save(course);
        eventPublisher.publishEvent(new CourseCreatedEvent(savedCourse.getId()));
        return savedCourse;
    }

    @Transactional
    public Course updateCourseData(Course course, CourseRequest request, List<Tag> validTags, String newImageUrl, String newImagePublicId) {

        // 💥 Vẫn xài TUYỆT CHIÊU PROXY cho nhẹ DB nhé (Cái này quá ngon không thể bỏ được)
        Category categoryRef = categoryRepository.getReferenceById(request.getCategoryId());

        // Cập nhật thông tin thẳng vào object truyền xuống
        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setPrice(request.getPrice());
        course.setOriginalPrice(request.getOriginalPrice());
        course.setLevel(request.getLevel());
        course.setCategory(categoryRef); // Gán Proxy

        course.clearTags();

        if (validTags != null && !validTags.isEmpty()) {
            validTags.forEach(course::addTag);
        }

        // Nếu có up ảnh mới thì đè URL và PublicID vào
        if (newImageUrl != null) {
            course.setImageUrl(newImageUrl);
            course.setImagePublicId(newImagePublicId);
        }

        Course updatedCourse = courseRepository.save(course);
        eventPublisher.publishEvent(new CourseUpdatedEvent(updatedCourse.getId()));

        return updatedCourse;
    }
}