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
            log.warn("🚨 Webhook thiếu thông tin Payload/Timestamp/Signature!");
            return false;
        }

        try {
            NotificationRequestSignatureVerifier verifier = new NotificationRequestSignatureVerifier(apiSecret);

            long validForSeconds = 300L;

            return verifier.verifySignature(rawPayload, timestamp, expectedSignature, validForSeconds);

        } catch (Exception e) {
            log.error("🚨 Lỗi xác thực Webhook: Chữ ký giả mạo hoặc đã quá hạn 5 phút (Replay Attack)!", e);
            return false;
        }
    }
}