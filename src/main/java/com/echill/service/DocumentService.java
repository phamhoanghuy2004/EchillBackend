package com.echill.service;

import com.echill.constant.CloudinaryFolder;
import com.echill.dto.response.DocumentResponse;
import com.echill.entity.Lesson;
import com.echill.entity.Document;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.mapper.DocumentMapper;
import com.echill.repository.DocumentRepository;
import com.echill.repository.LessonRepository;
import com.echill.service.persistence.DocumentPersistenceService;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DocumentService {

    LessonRepository lessonRepository;
    CloudinaryService cloudinaryService;
    DocumentPersistenceService documentPersistenceService;
    DocumentMapper documentMapper;
    DocumentRepository documentRepository;

    public DocumentResponse uploadDocument(Long lessonId, String title, MultipartFile file) {

        if(file == null || file.isEmpty()){
            throw new AppException(ErrorEnum.DOCUMENT_REQUIRED);
        }

        Lesson lesson = lessonRepository.findByIdForOwnershipCheck(lessonId)
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

        SecurityUtils.validateOwnership(lesson.getCourse().getTeacher().getId());

        // 2. Upload lên mây (Tốn thời gian nhưng DB không bị ảnh hưởng)
        Map<String, String> uploadResult = cloudinaryService.uploadDocument(file, CloudinaryFolder.DOCUMENT);
        String fileUrl = uploadResult.get("url");
        String publicId = uploadResult.get("publicId");

        if (fileUrl == null || publicId == null){
            throw new AppException(ErrorEnum.CANNOT_UPLOAD_FILE);
        }

        // 3. Đẩy ID xuống tầng Persistence lưu (Không truyền Object mồ côi nữa, truyền ID cho lẹ)
        Document savedDoc = documentPersistenceService.saveNewDocument(
                lesson.getId(), title, file.getContentType(), fileUrl, publicId
        );

        // 4. Trả về đúng 1 cục DocumentResponse nhỏ xíu, payload siêu mượt!
        return documentMapper.toDocumentResponse(savedDoc);
    }

    public void deleteDocument(Long documentId) {

        // 1. Tìm Document
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorEnum.DOCUMENT_NOT_FOUND));

        // 2. Dùng câu Query Tối Ưu Mới để check quyền
        Lesson lesson = lessonRepository.findByIdForOwnershipCheck(document.getLesson().getId())
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

        // Check quyền
        SecurityUtils.validateOwnership(lesson.getCourse().getTeacher().getId());

        // 3. Gọi xuống Persistence để xóa Data
        documentPersistenceService.deleteDocument(document);
    }

    public List<DocumentResponse> getDocumentsByLessonId(Long lessonId) {

        // 1. Kiểm tra tồn tại (Fail-fast cực nhanh bằng hàm COUNT, không lôi data lên RAM)
        if (!lessonRepository.existsById(lessonId)) {
            throw new AppException(ErrorEnum.LESSON_NOT_FOUND);
        }

        // 2. Bắn trực tiếp vào bảng Document lôi danh sách ra
        List<Document> documents = documentRepository.findByLessonId(lessonId);

        // 3. Map sang Response và trả về
        return documents.stream()
                .map(documentMapper::toDocumentResponse)
                .toList(); // Dùng .toList() của Java 16+ cho code hiện đại và sạch sẽ
    }

}