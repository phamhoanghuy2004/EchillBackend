package com.echill.service.persistence;

import com.echill.dto.request.CloudinaryWebhookPayload;
import com.echill.dto.request.SaveVideoDraftRequest;
import com.echill.dto.response.LessonResponse;
import com.echill.entity.Lesson;
import com.echill.event.CourseUpdatedEvent;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.WebhookRetryException;
import com.echill.mapper.LessonMapper;
import com.echill.repository.LessonRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.MDC;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonPersistenceService {

    LessonRepository lessonRepository;
    LessonMapper lessonMapper;
    ApplicationEventPublisher eventPublisher;

    @Transactional
    public LessonResponse saveVideoDraft(Long lessonId, SaveVideoDraftRequest request) {
        if (request.getDurationSeconds() == null || request.getDurationSeconds() <= 0) {
            log.error("❌ Lỗi dữ liệu: Thời lượng video không hợp lệ ({}) cho Lesson ID: {}", request.getDurationSeconds(), lessonId);
            throw new AppException(ErrorEnum.INVALID_VIDEO_DURATION);
        }

        Lesson lesson = lessonRepository.findByIdWithCourseAndTeacherAndDocuments(lessonId)
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

        SecurityUtils.validateOwnership(lesson.getCourse().getTeacher().getId());

        lesson.startVideoProcessing(request.getPublicVideoId(), request.getRawUrl(), request.getDurationSeconds());
        lessonRepository.save(lesson);

        log.info("✅ Đã chốt file nháp cho Lesson ID {}. Trạng thái: PROCESSING", lessonId);

        return lessonMapper.toLessonResponse(lesson);
    }

    @Transactional
    public Lesson processCloudinaryWebhook(CloudinaryWebhookPayload payload) {
        String publicId = payload.getPublicId();
        if (publicId == null || publicId.trim().isEmpty()) {
            throw new WebhookRetryException("Webhook thiếu public_id.");
        }

        MDC.put("publicId", publicId);

        try {
            Lesson lesson = lessonRepository.findByPublicVideoId(publicId)
                    .orElseThrow(() -> new WebhookRetryException("Chưa tìm thấy Lesson. Ép Cloudinary Retry!"));

            if (lesson.isVideoReady()) {
                log.info("♻️ Webhook Duplicate: Video đã READY từ trước.");
                return null;
            }

            if (!lesson.isVideoProcessing()) {
                log.warn("Trạng thái không hợp lệ: {}", lesson.getVideoStatus());
                return null;
            }

            if ("eager_error".equals(payload.getNotificationType())) {
                log.error("❌ Cloudinary báo lỗi convert. Đưa về FAILED.");
                lesson.failVideoProcessing();
                return lessonRepository.save(lesson);
            }

            String hlsUrl = null;
            if (payload.getEager() != null) {
                hlsUrl = payload.getEager().stream()
                        .filter(item -> "m3u8".equalsIgnoreCase(item.getFormat()))
                        .map(CloudinaryWebhookPayload.EagerItem::getSecureUrl)
                        .findFirst()
                        .orElse(null);
            }

            if (hlsUrl == null) {
                throw new WebhookRetryException("Không tìm thấy link m3u8 trong mảng eager. Retry!");
            }

            lesson.finishVideoProcessing(hlsUrl);
            Lesson savedLesson = lessonRepository.save(lesson);

            log.info("🚀 Hoàn tất Webhook, chuyển sang READY thành công!");
            eventPublisher.publishEvent(new CourseUpdatedEvent(savedLesson.getCourse().getId()));

            return savedLesson;

        } finally {
            MDC.remove("publicId");
        }
    }
}
