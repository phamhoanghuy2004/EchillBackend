package com.echill.config;

import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisRateLimiter {
    StringRedisTemplate redisTemplate;

    /**
     * Hàm kiểm tra giới hạn chung cho mọi API.
     * Nếu vượt giới hạn, nó sẽ tự động ném ra Exception chặn đứng API.
     *
     * @param actionKey   Tên hành động (VD: "generate_video_sig", "send_otp", "login_fail")
     * @param identifier  Định danh người dùng (VD: userId, email, hoặc địa chỉ IP)
     * @param maxRequests Số lần tối đa cho phép
     * @param timeWindow  Khoảng thời gian
     * @param timeUnit    Đơn vị thời gian (Giờ, Phút, Giây...)
     */
    public void checkLimit(String actionKey, String identifier, int maxRequests, long timeWindow, TimeUnit timeUnit) {

        // 1. Tạo Key duy nhất cho hành động này
        String redisKey = String.format("rate_limit:%s:%s", actionKey, identifier);

        // 2. Tăng biến đếm
        Long requestCount = redisTemplate.opsForValue().increment(redisKey);

        // 3. Set thời gian tự hủy (TTL) cho lần đầu tiên
        if (requestCount != null && requestCount == 1) {
            redisTemplate.expire(redisKey, timeWindow, timeUnit);
        }

        // 4. Nếu vượt rào -> Rút thẻ đỏ ngay!
        if (requestCount != null && requestCount > maxRequests) {
            log.warn("🚨 [RATE LIMIT EXCEEDED] Hành động: '{}' | Định danh: '{}' | Số lần: {}/{}",
                    actionKey, identifier, requestCount, maxRequests);

            throw new AppException(ErrorEnum.RATE_LIMIT_EXCEEDED);
        }
    }
}
