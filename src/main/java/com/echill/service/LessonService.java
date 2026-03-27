package com.echill.service;

import com.echill.constant.CloudinaryFolder;
import com.echill.dto.request.LessonCreationRequest;
import com.echill.dto.request.SaveVideoDraftRequest;
import com.echill.dto.response.LessonResponse;
import com.echill.entity.Course;
import com.echill.entity.Document;
import com.echill.entity.Lesson;
import com.echill.entity.enums.FileType;
import com.echill.entity.enums.VideoStatus;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.LessonMapper;
import com.echill.repository.CourseRepository;
import com.echill.repository.DocumentRepository;
import com.echill.repository.LessonRepository;
import com.echill.service.CloudinaryService;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonService {
    LessonRepository lessonRepository;
    LessonMapper lessonMapper;
    CourseRepository courseRepository;
    DocumentRepository documentRepository;
    CloudinaryService cloudinaryService;

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

    @Transactional
    public LessonResponse uploadDocument(Long lessonId, String title, MultipartFile file) {
        // 1. Tìm Lesson
        Lesson lesson = lessonRepository.findByIdWithCourseAndDocuments(lessonId)
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

        // 2. Chế độ bảo vệ: Kiểm tra xem User đang thao tác có phải là chủ khóa học không?
        checkAuthority(lesson.getCourse(), "thêm tài liệu");

        // 3. Đẩy lên Cloudinary
        String fileUrl = cloudinaryService.uploadDocument(file, CloudinaryFolder.DOCUMENT);

        // 4. Xác định FileType
        FileType fileType = FileType.PDF;
        String contentType = file.getContentType();
        if (contentType != null && (contentType.contains("word") || contentType.contains("msword"))) {
            fileType = FileType.WORD;
        }

        // 5. Tạo Document entity
        Document document = Document.builder()
                .title(title)
                .fileUrl(fileUrl)
                .fileType(fileType)
                .build();

        lesson.addDocument(document);
        lessonRepository.save(lesson);
        log.info("Đã tải lên tài liệu mới cho bài học ID: {}, Document ID: {}", lessonId, document.getId());

        // 6. Trả về Response
        return lessonMapper.toLessonResponse(lesson);
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        // 1. Tìm Document
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorEnum.DOCUMENT_NOT_FOUND));

        // 2. Kiểm tra quyền của cha nó (Lesson -> Course)
        Lesson lesson = lessonRepository.findByIdWithCourseAndDocuments(document.getLesson().getId())
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));
        checkAuthority(lesson.getCourse(), "xóa tài liệu");

        // 3. Xóa trên Cloudinary
        cloudinaryService.deleteDocument(document.getFileUrl());

        // 4. Xóa trong Database
        lesson.removeDocument(document);
        lessonRepository.save(lesson);
        log.info("Đã xóa tài liệu ID: {} khỏi bài học ID: {}", documentId, lesson.getId());
    }

    @Transactional
    public LessonResponse updateLesson(Long lessonId, LessonCreationRequest request) {
        // 1. Tìm Lesson
        Lesson lesson = lessonRepository.findByIdWithCourseAndDocuments(lessonId)
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

        // 2. Chế độ bảo vệ: Kiểm tra xem User đang thao tác có phải là chủ khóa học không?
        checkAuthority(lesson.getCourse(), "cập nhật bài học");

        // 3. Cập nhật thông tin nội dung
        lesson.setTitle(request.getTitle());
        lesson.setContent(request.getContent());
        lesson.setDisplayOrder(request.getDisplayOrder());
        lesson.setIsPreview(request.getIsPreview());

        // 4. Lưu database
        lessonRepository.save(lesson);
        log.info("Đã cập nhật bài học ID: {} thành công", lessonId);

        // 5. Trả về Response
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
