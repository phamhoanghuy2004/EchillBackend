package com.echill.service;

import com.echill.dto.request.LessonCreationRequest;
import com.echill.dto.response.LessonResponse;
import com.echill.entity.Course;
import com.echill.entity.Lesson;
import com.echill.entity.Document;
import com.echill.entity.Tag;
import com.echill.event.CourseUpdatedEvent;
import com.echill.event.LessonUpdatedEvent;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.LessonMapper;
import com.echill.repository.CourseRepository;
import com.echill.repository.LessonRepository;
import com.echill.repository.TagRepository;
import com.echill.util.SecurityUtils;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    TagRepository tagRepository;
    ApplicationEventPublisher eventPublisher;

    @Transactional
    public LessonResponse createLesson(LessonCreationRequest request) {
        Course course = courseRepository.findByIdWithTeacher(request.getCourseId())
                .orElseThrow(() -> new AppException(TeacherErrorEnum.COURSE_NOT_FOUND));

        SecurityUtils.validateOwnership(course.getTeacher().getId());

        Lesson lesson = Lesson.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .displayOrder(request.getDisplayOrder())
                .isPreview(request.getIsPreview())
                .course(course)
                .build();

        List<Tag> validTags = new ArrayList<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            if (request.getTagIds().stream().distinct().count() > 3) {
                throw new AppException(TeacherErrorEnum.LESSON_TAGS_LIMIT_EXCEEDED);
            }
            validTags = tagRepository.findAllById(request.getTagIds());
            if (validTags.size() != request.getTagIds().size()) {
                throw new AppException(TeacherErrorEnum.TAG_NOT_FOUND);
            }
        }

        lesson.getTags().addAll(validTags);

        lessonRepository.save(lesson);
        log.info("Đã tạo mới khung bài học (Text) thành công, Lesson ID: {}", lesson.getId());

        if (!validTags.isEmpty()) {
            boolean updated = false;
            for (Tag tag : validTags) {
                if (!course.getTags().contains(tag)) {
                    course.addTag(tag);
                    updated = true;
                }
            }
            if (updated) {
                courseRepository.save(course);
            }
        }

        // 🔴 ATOMIC UPDATE: Tăng biến đếm an toàn tuyệt đối
        courseRepository.incrementLessonCount(course.getId());

        eventPublisher.publishEvent(new CourseUpdatedEvent(course.getId()));

        return lessonMapper.toLessonResponse(lesson);
    }

    @Transactional
    public LessonResponse updateLesson(Long lessonId, LessonCreationRequest request) {
        // 1. Tìm Lesson
        Lesson lesson = lessonRepository.findByIdWithCourseAndTeacherAndDocuments(lessonId)
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

        SecurityUtils.validateOwnership(lesson.getCourse().getTeacher().getId());

        Long courseId = lesson.getCourse().getId();

        // 3. Cập nhật thông tin nội dung
        lesson.setTitle(request.getTitle());
        lesson.setContent(request.getContent());
        lesson.setDisplayOrder(request.getDisplayOrder());
        lesson.setIsPreview(request.getIsPreview());

        List<Tag> validTags = new ArrayList<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            if (request.getTagIds().stream().distinct().count() > 3) {
                throw new AppException(TeacherErrorEnum.LESSON_TAGS_LIMIT_EXCEEDED);
            }
            validTags = tagRepository.findAllById(request.getTagIds());
            if (validTags.size() != request.getTagIds().size()) {
                throw new AppException(TeacherErrorEnum.TAG_NOT_FOUND);
            }
        }

        lesson.getTags().clear();
        lesson.getTags().addAll(validTags);

        // 4. Lưu database
        lessonRepository.save(lesson);
        log.info("Đã cập nhật bài học ID: {} thành công", lessonId);

        Course course = lesson.getCourse();
        if (!validTags.isEmpty()) {
            boolean updated = false;
            for (Tag tag : validTags) {
                if (!course.getTags().contains(tag)) {
                    course.addTag(tag);
                    updated = true;
                }
            }
            if (updated) {
                courseRepository.save(course);
            }
        }

        eventPublisher.publishEvent(new LessonUpdatedEvent(lessonId));

        eventPublisher.publishEvent(new CourseUpdatedEvent(courseId));

        // 5. Trả về Response
        return lessonMapper.toLessonResponse(lesson);
    }

    @Transactional
    public void deleteLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdWithCourseAndTeacherAndDocuments(lessonId)
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

        SecurityUtils.validateOwnership(lesson.getCourse().getTeacher().getId());

        Long courseId = lesson.getCourse().getId();

        lessonRepository.delete(lesson);
        log.info("Đã xóa bài học ID: {} thành công", lessonId);

        // 🔴 ATOMIC UPDATE: Giảm biến đếm an toàn tuyệt đối
        courseRepository.decrementLessonCount(courseId);

        eventPublisher.publishEvent(new CourseUpdatedEvent(courseId));
    }

    public com.echill.dto.response.DocumentChatResponse chatWithLesson(Long lessonId, String question) {
        Lesson lesson = lessonRepository.findByIdWithCourseAndTeacherAndDocuments(lessonId)
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

        java.util.Set<Document> documents = lesson.getDocuments();
        if (documents == null || documents.isEmpty()) {
            return com.echill.dto.response.DocumentChatResponse.builder()
                    .answer("Bài học này hiện tại chưa có tài liệu nào để AI có thể tham khảo.")
                    .build();
        }

        List<String> sources = new java.util.ArrayList<>();
        for (Document doc : documents) {
            String cleanTitle = doc.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
            if (!cleanTitle.toLowerCase().endsWith(".pdf")) {
                cleanTitle += ".pdf";
            }
            sources.add(cleanTitle);
        }

        String token = generateRAGChatbotToken();
        String chatUrl = "https://phamhoanghuy-ragchatbot.hf.space/api/chat/";
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("question", question);
        body.put("source", sources);
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

    // ===== ADAPTIVE LEARNING (Bước 4) =====

    /**
     * Tìm bài học (trong khóa đã mua) dạy về Tag bị hổng.
     * Query 1 lần duy nhất join lesson_tags → lessons → courses → enrollments.
     *
     * @param userId ID của học viên
     * @param tagId  ID của Tag bị hổng
     * @return Optional<Lesson> - Bài học phù hợp nhất (theo displayOrder)
     */
    @Transactional(readOnly = true)
    public Optional<Lesson> findLessonForGapTag(Long userId, Long tagId) {
        List<Lesson> lessons = lessonRepository.findLessonsByTagAndEnrolledUser(userId, tagId);
        return lessons.stream().findFirst();
    }

}
