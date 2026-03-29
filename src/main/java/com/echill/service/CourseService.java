package com.echill.service;

import com.echill.constant.CloudinaryFolder;
import com.echill.dto.request.CourseRequest;
import com.echill.dto.response.CourseResponse;
import com.echill.dto.response.LessonResponse;
import com.echill.entity.Course;
import com.echill.service.persistence.CoursePersistenceService;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseService {

    CloudinaryService cloudinaryService;
    CoursePersistenceService coursePersistenceService;

    // 💥 KHÔNG CÓ @Transactional Ở ĐÂY NỮA
    public CourseResponse createCourse(CourseRequest request, MultipartFile file) {
        // 1. Lấy trực tiếp ID từ JWT (Không cần query Username)
        Long teacherId = SecurityUtils.getCurrentUserId();

        // 2. Upload ảnh (Network I/O) - Nếu tốn 3s thì MySQL vẫn rảnh rang
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            imageUrl = cloudinaryService.uploadImage(file, CloudinaryFolder.COURSE_IMAGE);
        }

        // 3. Đẩy xuống Persistence để lưu DB thật nhanh
        Course course = coursePersistenceService.saveNewCourse(teacherId, request, imageUrl);

        // 4. Map trả về
        return mapToResponse(course);
    }

    public List<CourseResponse> getAllCoursesByTeacher() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return coursePersistenceService.getAllCoursesByTeacherId(teacherId).stream()
                .map(this::mapToResponse)
                .toList(); // Dùng .toList() của Java 16+ cho lẹ, bỏ Collectors.toList() đi
    }

    public CourseResponse getCourseById(Long id) {
        Course course = coursePersistenceService.getCourseById(id);
        return mapToResponse(course);
    }

    // Hàm Map giờ đã an toàn tuyệt đối, vì dữ liệu đã được JOIN FETCH kéo lên hết rồi
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
                .build();
    }
}