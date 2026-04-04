package com.echill.controller;

import com.echill.config.CloudinarySignatureValidator;
import com.echill.entity.Lesson;
import com.echill.event.CourseUpdatedEvent;
import com.echill.service.persistence.LessonPersistenceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/webhook/cloudinary")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryWebhookController {

    LessonPersistenceService lessonPersistenceService;
    SimpMessagingTemplate messagingTemplate;
    CloudinarySignatureValidator signatureValidator;
    ObjectMapper objectMapper;


    @PostMapping
    public ResponseEntity<Void> handleCloudinaryNotification(
            @RequestHeader(value = "X-Cld-Signature", required = false) String signature,
            @RequestHeader(value = "X-Cld-Timestamp", required = false) String timestamp,
            @RequestBody String rawPayload
    ) {
        if (!signatureValidator.verifySignature(rawPayload, timestamp, signature)) {
            log.warn("Phát hiện Webhook sai chữ ký! Từ chối phục vụ.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(rawPayload, new TypeReference<>() {});
            String notificationType = (String) payload.get("notification_type");

            if ("eager".equals(notificationType)) {
                Lesson updatedLesson = lessonPersistenceService.processCloudinaryWebhook(payload);

                if (updatedLesson != null) {
                    messagingTemplate.convertAndSend("/topic/lessons/" + updatedLesson.getId(), Map.of(
                            "lessonId", updatedLesson.getId(),
                            "status", "READY",
                            "hlsUrl", updatedLesson.getHlsUrl(),
                            "durationSeconds", updatedLesson.getDurationSeconds()
                    ));
                    log.info("📡 Đã bắn WebSocket báo READY cho Frontend, Lesson ID: {}", updatedLesson.getId());
                }
            }
            return ResponseEntity.ok().build();

        } catch (DataAccessException | TransactionException e) {
            log.error("Lỗi Database. Ép Cloudinary gọi lại (Retry) bằng mã 500", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Lỗi Parsing Payload. Bỏ qua để tránh Spam", e);
            return ResponseEntity.ok().build();
        }
    }
}