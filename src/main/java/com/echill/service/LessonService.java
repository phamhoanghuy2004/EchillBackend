package com.echill.service;

import com.echill.dto.request.LessonCreationRequest;
import com.echill.dto.request.SaveVideoDraftRequest;
import com.echill.dto.response.LessonResponse;
import com.echill.entity.Course;
import com.echill.entity.Lesson;
import com.echill.entity.enums.VideoStatus;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.LessonMapper;
import com.echill.repository.CourseRepository;
import com.echill.repository.LessonRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonService {
    LessonRepository lessonRepository;
    LessonMapper lessonMapper;
    CourseRepository courseRepository;

    @Transactional
    public LessonResponse saveVideoDraft(Long lessonId, SaveVideoDraftRequest request) {

        // 1. Tìm bài học trong DB
        Lesson lesson = lessonRepository.findByIdWithCourseAndDocuments(lessonId)
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

        checkAuthority(lesson.getCourse(), "chỉnh sửa khóa học");

        // 2. Cập nhật thông tin video
        lesson.setPublicVideoId(request.getPublicVideoId());
        lesson.setRawUrl(request.getRawUrl());

        // 💥 QUAN TRỌNG: Phải chuyển trạng thái sang PROCESSING để đón Webhook
        lesson.setVideoStatus(VideoStatus.PROCESSING);

        // 3. Lưu vào Database
        lessonRepository.save(lesson);
        log.info("Đã lưu bản nháp video (PROCESSING) cho bài học ID: {}", lessonId);

        // 4. Trả về DTO cho Frontend để cập nhật UI ngay lập tức
        return lessonMapper.toLessonResponse(lesson);
    }

    @Transactional
    public LessonResponse createLesson(LessonCreationRequest request) {
        // 1. Tìm Course cha
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new AppException(TeacherErrorEnum.COURSE_NOT_FOUND));

        // 2. BẢO MẬT: Kiểm tra quyền
        checkAuthority(course, "tạo bài học");

        // 3. Khởi tạo Lesson
        Lesson lesson = Lesson.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .displayOrder(request.getDisplayOrder())
                .isPreview(request.getIsPreview())
                // 💥 GIẢI PHÁP: Chỉ cần set chiều từ Con -> Cha là Database tự hiểu!
                .course(course)
                .build();

        // XÓA DÒNG NÀY ĐỂ TRÁNH KÉO 100 BÀI HỌC CŨ LÊN RAM
        // course.addLesson(lesson);

        // 4. Lưu xuống DB (Chỉ tốn đúng 1 lệnh INSERT, siêu nhanh)
        lessonRepository.save(lesson);
        log.info("Đã tạo mới khung bài học (Text) thành công, Lesson ID: {}", lesson.getId());

        // 5. Map ra Response trả về
        return lessonMapper.toLessonResponse(lesson);
    }

    private void checkAuthority (Course course, String action){
        // 💥 2. BẢO MẬT: Kiểm tra xem User đang thao tác có phải là chủ khóa học không?
        Long currentUserId = SecurityUtils.getCurrentUserId();
        // Sửa hàm getTeacher() thành hàm lấy user của bạn trong Course entity
        Long courseOwnerId = course.getTeacher().getId();

        if (!currentUserId.equals(courseOwnerId)) {
            log.warn("🚨 [BẢO MẬT] User ID: {} định {} trái phép vào Course ID: {} của User ID: {}",
                    currentUserId, action, course.getId(), courseOwnerId);
            throw new AppException(ErrorEnum.UNAUTHORIZED); // Bắn lỗi 403
        }
    }
}
