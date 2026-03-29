package com.echill.config;
import com.cloudinary.api.signing.NotificationRequestSignatureVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CloudinarySignatureValidator {

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    public boolean verifySignature(String rawPayload, String timestamp, String expectedSignature) {
        if (rawPayload == null || timestamp == null || expectedSignature == null) {
            return false;
        }
        try {
            NotificationRequestSignatureVerifier verifier = new NotificationRequestSignatureVerifier(apiSecret);
            return verifier.verifySignature(rawPayload, timestamp, expectedSignature, 7200L);

        } catch (Exception e) {
            log.error("🚨 Lỗi xác thực Webhook Cloudinary: Chữ ký không hợp lệ!", e);
            return false;
        }
    }
}