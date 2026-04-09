package com.echill.controller;

import com.echill.config.CloudinarySignatureValidator;
import com.echill.dto.request.CloudinaryWebhookPayload;
import com.echill.entity.Lesson;
import com.echill.exception.WebhookRetryException;
import com.echill.service.persistence.LessonPersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
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
        log.info("Received cloudinary webhook notification");
        log.info("🔥 [CLOUDINARY RAW PAYLOAD]: {}", rawPayload);
        if (!signatureValidator.verifySignature(rawPayload, timestamp, signature)) {
            log.warn("🚨 Sai chữ ký Webhook!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CloudinaryWebhookPayload payload = objectMapper.readValue(rawPayload, CloudinaryWebhookPayload.class);

            String notificationType = payload.getNotificationType();

            if ("eager".equals(notificationType) || "eager_error".equals(notificationType)) {
                Lesson updatedLesson = lessonPersistenceService.processCloudinaryWebhook(payload);

                if (updatedLesson != null) { // khác null là đã có thay đổi thì mới bắn socket
                    messagingTemplate.convertAndSend("/topic/lessons/" + updatedLesson.getId(), Map.of(
                            "lessonId", updatedLesson.getId(),
                            "status", updatedLesson.getVideoStatus(),
                            "hlsUrl", updatedLesson.getHlsUrl() != null ? updatedLesson.getHlsUrl() : ""
                    ));
                    log.info("📡 Đã bắn WebSocket báo {} cho Frontend, Lesson ID: {}", updatedLesson.getVideoStatus(), updatedLesson.getId());
                }
            }
            return ResponseEntity.ok().build();

        } catch (WebhookRetryException e) {
            log.warn("⏳ Race Condition: {}. Trả về 404 để ép Cloudinary Retry.", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        catch (PessimisticLockingFailureException e) {
            log.warn("⚔️ Đụng độ Database Lock (2 Webhook vào cùng lúc). Trả 409 Conflict giãn cách Retry.");
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        catch (DataAccessException | TransactionException e) {
            log.error("💥 Lỗi Database nội bộ. Trả 500 để Cloudinary Retry.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        catch (Exception e) {
            log.error("❌ Lỗi Parsing Payload hoặc Ngoại lệ không xác định. Trả 200 OK để BỎ QUA tránh Spam.", e);
            return ResponseEntity.ok().build();
        }
    }
}