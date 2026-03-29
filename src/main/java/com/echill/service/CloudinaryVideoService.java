package com.echill.service;

import com.cloudinary.Cloudinary;
import com.echill.config.RedisRateLimiter;
import com.echill.constant.CloudinaryFolder;
import com.echill.dto.response.CloudinarySignatureResponse;
import com.echill.entity.Lesson;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.LessonRepository;
import com.echill.service.persistence.LessonPersistenceService;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryVideoService {

    Cloudinary cloudinary;
    RedisRateLimiter rateLimiter;

    LessonRepository lessonRepository;

    @Value("${cloudinary.webhook-url}")
    @NonFinal
    String webhookUrl;

    public CloudinarySignatureResponse generateVideoUploadSignature(Long lessonId) {
        Long userId = SecurityUtils.getCurrentUserId();
        rateLimiter.checkLimit("video_signature", String.valueOf(userId), 15, 1, TimeUnit.HOURS);

        Lesson lesson = lessonRepository.findByIdForOwnershipCheck(lessonId)
                .orElseThrow(() -> new AppException(ErrorEnum.LESSON_NOT_FOUND));

        SecurityUtils.validateOwnership(lesson.getCourse().getTeacher().getId());

        if (lesson.isVideoProcessing()) {
            throw new AppException(ErrorEnum.VIDEO_IS_PROCESSING);
        }

        String publicId = "lesson_" + lessonId + "_" + UUID.randomUUID().toString().substring(0, 8);

        long timestamp = Instant.now().getEpochSecond();
        Map<String, Object> params = new HashMap<>();
        params.put("folder", CloudinaryFolder.LESSION_VIDEO);
        params.put("public_id", publicId);
        params.put("timestamp", timestamp);
        params.put("eager", "sp_full_hd/m3u8");
        params.put("eager_async", true);
        params.put("notification_url", webhookUrl);

        String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret);

        return CloudinarySignatureResponse.builder()
                .signature(signature)
                .timestamp(timestamp)
                .apiKey(cloudinary.config.apiKey)
                .cloudName(cloudinary.config.cloudName)
                .folder(CloudinaryFolder.LESSION_VIDEO)
                .publicId(publicId)
                .eager("sp_full_hd/m3u8")
                .eagerAsync(true)
                .notificationUrl(webhookUrl)
                .build();
    }
}
