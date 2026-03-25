package com.echill.controller;

import com.echill.entity.Lesson;
import com.echill.entity.enums.VideoStatus;
import com.echill.repository.LessonRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webhook/cloudinary")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryWebhookController {
    LessonRepository lessonRepository;

    @PostMapping
    public ResponseEntity<Void> handleCloudinaryNotification(@RequestBody Map<String, Object> payload) {
        try {
            String notificationType = (String) payload.get("notification_type");
            String publicId = (String) payload.get("public_id");

            // 💥 Chỉ quan tâm đến event 'eager' (tức là đã convert HLS xong)
            if ("eager".equals(notificationType)) {
                log.info("✅ Nhận webhook HLS xử lý xong cho video: {}", publicId);

                // Tìm Lesson trong DB dựa vào publicId do Frontend lưu trước đó
                Lesson lesson = lessonRepository.findByPublicVideoId(publicId).orElse(null);

                if (lesson != null) {
                    // 1. Móc mảng 'eager' ra để lấy link HLS (.m3u8)
                    List<Map<String, Object>> eagerArray = (List<Map<String, Object>>) payload.get("eager");
                    if (eagerArray != null && !eagerArray.isEmpty()) {
                        String hlsSecureUrl = (String) eagerArray.get(0).get("secure_url");
                        lesson.setHlsUrl(hlsSecureUrl);
                    }

                    // 2. Móc 'duration' ra để cập nhật thời lượng (nếu Cloudinary có trả về)
                    // Lưu ý: Có trường hợp Cloudinary trả ở payload gốc, ta check phòng hờ
                    if (payload.containsKey("duration")) {
                        Object durationObj = payload.get("duration");
                        if (durationObj instanceof Number num) {
                            lesson.setDurationSeconds(Math.round(num.doubleValue()));
                        }
                    }

                    // 3. Đánh dấu hoàn tất và lưu DB
                    lesson.setVideoStatus(VideoStatus.READY);
                    lessonRepository.save(lesson);

                    log.info("🎉 Bài học ID: {} đã cập nhật HLS thành công!", lesson.getId());
                } else {
                    log.warn("⚠️ Bỏ qua Webhook: Không tìm thấy Lesson nào có publicVideoId: {}", publicId);
                }
            }

            // Luôn trả về 200 OK để Cloudinary không spam gửi lại
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ Lỗi nghiêm trọng khi xử lý Webhook Cloudinary", e);
            return ResponseEntity.ok().build();
        }
    }
}
