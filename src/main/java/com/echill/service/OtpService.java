package com.echill.service;

import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OtpService {
    RedisTemplate<String, String> redisTemplate;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String OTP_PREFIX = "otp:register:";
    private static final String RESET_OTP_PREFIX = "otp:forgotPassword:";
    private static final int OTP_EXPIRATION_MINUTES = 5;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;

    private String getRedisKey(String email, boolean isForgot) {
        return isForgot ? RESET_OTP_PREFIX + email : OTP_PREFIX + email;
    }

    public void validateOtp(String email, String inputOtp, boolean isForgot) {
        String redisKey = getRedisKey(email, isForgot);
        String savedOtp = redisTemplate.opsForValue().get(redisKey);

        if (savedOtp == null) throw new AppException(ErrorEnum.OTP_EXPIRED);
        if (!savedOtp.equals(inputOtp)) throw new AppException(ErrorEnum.OTP_INCORRECT);
    }

    public void validateOtpResendCooldown(String email, boolean isForgot) {
        String redisKey = getRedisKey(email, isForgot);
        Long expireTime = redisTemplate.getExpire(redisKey);

        if (expireTime != null && expireTime > OTP_EXPIRATION_MINUTES * 60 - OTP_RESEND_COOLDOWN_SECONDS) {
            throw new AppException(ErrorEnum.PLEASE_WAIT_BEFORE_RESEND);
        }
    }

    public String generateAndSaveOtp(String email, boolean isForgot) {
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        redisTemplate.opsForValue().set(getRedisKey(email, isForgot), otp, Duration.ofMinutes(OTP_EXPIRATION_MINUTES));
        log.warn("Đã sinh ra OTP là: {}", otp);
        return otp; // Chỉ trả về OTP, không gọi EmailService nữa
    }

    public void clearOtp(String email, boolean isForgot) {
        redisTemplate.delete(getRedisKey(email, isForgot));
    }
}
