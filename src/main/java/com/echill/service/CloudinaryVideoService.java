package com.echill.service;

import com.cloudinary.Cloudinary;
import com.echill.config.RedisRateLimiter;
import com.echill.constant.CloudinaryFolder;
import com.echill.dto.response.CloudinarySignatureResponse;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryVideoService {
    Cloudinary cloudinary;
    RedisRateLimiter rateLimiter;

    public CloudinarySignatureResponse generateVideoUploadSignature() {
        Long userId = SecurityUtils.getCurrentUserId();
        rateLimiter.checkLimit("video_signature", String.valueOf(userId), 3, 1, TimeUnit.HOURS);

        long timestamp = Instant.now().getEpochSecond();
        Map<String, Object> params = new HashMap<>();

        // 1. Chỉ định thư mục
        params.put("folder", CloudinaryFolder.LESSION_VIDEO);
        params.put("timestamp", timestamp);

        // 2. Lệnh bất đồng bộ ép convert HLS
        params.put("eager", "sp_full_hd/m3u8");
        params.put("eager_async", true);

        // 3. Băm chữ ký bảo mật
        String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret);

        // 💥 4. Trả về DTO xịn xò
        return CloudinarySignatureResponse.builder()
                .signature(signature)
                .timestamp(timestamp)
                .apiKey(cloudinary.config.apiKey)
                .cloudName(cloudinary.config.cloudName)
                .eager("sp_full_hd/m3u8")
                .eagerAsync(true)
                .folder(CloudinaryFolder.LESSION_VIDEO)
                .build();
    }
}
