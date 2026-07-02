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

        Long courseId = lesson.getCourse().getId();

        // 2. Upload lên mây (Tốn thời gian nhưng DB không bị ảnh hưởng)
        Map<String, String> uploadResult = cloudinaryService.uploadDocument(file, CloudinaryFolder.DOCUMENT);
        String fileUrl = uploadResult.get("url");
        String publicId = uploadResult.get("publicId");

        if (fileUrl == null || publicId == null){
            throw new AppException(ErrorEnum.CANNOT_UPLOAD_FILE);
        }

        // 3. Đẩy ID xuống tầng Persistence lưu (Không truyền Object mồ côi nữa, truyền ID cho lẹ)
        Document savedDoc = documentPersistenceService.saveNewDocument(
                lesson.getId(), title, file.getContentType(), fileUrl, publicId, courseId
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

        Long courseId = lesson.getCourse().getId();
        Long lessonId = lesson.getId();

        // 3. Gọi xuống Persistence để xóa Data
        documentPersistenceService.deleteDocument(document, courseId, lessonId);

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

    public DocumentResponse getDocumentDetail(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorEnum.DOCUMENT_NOT_FOUND));
        return documentMapper.toDocumentResponse(document);
    }

    public com.echill.dto.response.DocumentChatResponse chatWithDocument(Long documentId, String question) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorEnum.DOCUMENT_NOT_FOUND));

        // Format source name: clean title and ensure it ends with .pdf
        String cleanTitle = document.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (!cleanTitle.toLowerCase().endsWith(".pdf")) {
            cleanTitle += ".pdf";
        }

        // Generate JWT token for RAG Chatbot
        String token = generateRAGChatbotToken();

        // Call chatbot chat API
        String chatUrl = "https://phamhoanghuy-ragchatbot.hf.space/api/chat/";
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("question", question);
        body.put("source", java.util.List.of(cleanTitle));
        body.put("model", "llama-3.1-8b-instant");

        byte[] jsonBytes;
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            jsonBytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            log.error("Lỗi khi serialize body cho RAG Chatbot: {}", e.getMessage(), e);
            throw new AppException(ErrorEnum.CANNOT_GET_AI_RESPONSE);
        }

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.setContentLength(jsonBytes.length);
        headers.add("Cookie", "access_token=" + token);

        org.springframework.http.HttpEntity<byte[]> entity = new org.springframework.http.HttpEntity<>(jsonBytes, headers);

        try {
            java.util.Map responseMap = restTemplate.postForObject(chatUrl, entity, java.util.Map.class);
            if (responseMap == null || !responseMap.containsKey("answer")) {
                throw new AppException(ErrorEnum.CANNOT_GET_AI_RESPONSE);
            }
            String answer = (String) responseMap.get("answer");
            return com.echill.dto.response.DocumentChatResponse.builder()
                    .answer(answer)
                    .build();
        } catch (Exception e) {
            log.error("Lỗi khi kết nối với RAG Chatbot: {}", e.getMessage(), e);
            throw new AppException(ErrorEnum.CANNOT_GET_AI_RESPONSE);
        }
    }

    private String generateRAGChatbotToken() {
        try {
            String signerKey = "Pb6zWmUnYq0Wk6O00k5yiQEb+U6PIY+6B1zmH84HMsLPApdlwj0sc3jecsI/Bu88";
            byte[] sharedKey = signerKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            com.nimbusds.jose.JWSSigner signer = new com.nimbusds.jose.crypto.MACSigner(sharedKey);
            
            com.nimbusds.jose.JWSHeader header = new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256);
            
            com.nimbusds.jwt.JWTClaimsSet claimsSet = new com.nimbusds.jwt.JWTClaimsSet.Builder()
                    .claim("user_id", 1)
                    .claim("roles", java.util.List.of("admin"))
                    .claim("user_name", "Hoang Huy")
                    .claim("type", "access")
                    .issueTime(new java.util.Date())
                    .expirationTime(java.util.Date.from(java.time.Instant.now().plusSeconds(900))) // 15 mins
                    .build();
            
            com.nimbusds.jose.Payload payload = new com.nimbusds.jose.Payload(claimsSet.toJSONObject());
            com.nimbusds.jose.JWSObject jwsObject = new com.nimbusds.jose.JWSObject(header, payload);
            
            jwsObject.sign(signer);
            return jwsObject.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo JWT cho RAG Chatbot: " + e.getMessage(), e);
        }
    }
}