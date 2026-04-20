package com.echill.job;

import com.echill.repository.InvalidatedTokenRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TokenCleanupJob {
    InvalidatedTokenRepository invalidatedTokenRepository;

    @Scheduled(cron = "0 40 2 * * ?")
    public void cleanUpExpiredTokens() {
        log.info("⏳ Bắt đầu Job dọn dẹp các token rác đã hết hạn trong Blacklist...");

        // Gọi hàm xóa và lấy về số lượng đã xóa
        int deletedCount = invalidatedTokenRepository.deleteAllExpiredSince(new Date());

        log.info("✅ Dọn dẹp hoàn tất! Đã giải phóng {} token rác khỏi Database.", deletedCount);
    }
}
