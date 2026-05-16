package com.echill.service.persistence;

import com.echill.entity.Document;
import com.echill.entity.Lesson;
import com.echill.entity.enums.FileType;
import com.echill.event.CourseUpdatedEvent;
import com.echill.event.LessonUpdatedEvent;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.DocumentRepository;
import com.echill.repository.LessonRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DocumentPersistenceService {

    DocumentRepository documentRepository;
    LessonRepository lessonRepository;
    ApplicationEventPublisher eventPublisher;

    @Transactional
    public Document saveNewDocument(Long lessonId, String title, String contentType, String fileUrl, String publicId, Long courseId) {

        // 1. Phân loại File
        FileType fileType = FileType.PDF;
        if (contentType != null && (contentType.contains("word") ||
                contentType.contains("msword") ||
                contentType.contains("officedocument"))) {
            fileType = FileType.WORD;
        }

        // 2. 🟢 Lấy trực tiếp Lesson thực sự từ DB (Bỏ Proxy đi)
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

        // 3. Tạo mới Document
        Document document = Document.builder()
                .title(title)
                .fileUrl(fileUrl)
                .documentPublicId(publicId)
                .fileType(fileType)
                .lesson(lesson) // Móc khóa ngoại vào Bài học bằng Proxy
                .build();

        // 4. Lưu trực tiếp vào bảng Document (Thay vì lưu gián tiếp qua Lesson.save như cũ)
        document = documentRepository.save(document);

        lesson.incrementVersion();

        log.info("Đã lưu tài liệu ID {} cho Bài học ID {}", document.getId(), lessonId);

        eventPublisher.publishEvent(new CourseUpdatedEvent(courseId));
        eventPublisher.publishEvent(new LessonUpdatedEvent(lessonId));

        return document;
    }

    @Transactional
    public void deleteDocument(Document document, Long courseId) {

        // 💥 Xóa trực tiếp entity Document ra khỏi Database.
        // Lệnh này sẽ sinh ra đúng 1 câu: DELETE FROM document WHERE id = ?
        documentRepository.delete(document);

        log.info("Đã xóa vĩnh viễn tài liệu ID: {} khỏi Database. File trên Cloudinary sẽ được Cron Job dọn dẹp sau.", document.getId());

        eventPublisher.publishEvent(new CourseUpdatedEvent(courseId));
    }
}