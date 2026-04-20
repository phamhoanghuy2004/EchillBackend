package com.echill.service;

import com.echill.entity.StudentProfile;
import com.echill.entity.User;
import com.echill.entity.enums.Level;
import com.echill.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserLevelService {

    private final StudentProfileRepository studentProfileRepository;

    /**
     * Logic chính: Xử lý thăng hạng dựa trên kết quả bài thi.
     * Đảm bảo nguyên tắc: CHỈ CÓ TĂNG, KHÔNG CÓ GIẢM.
     */
    @Transactional
    public void processLevelEvolution(Long userId, Map<Long, Double> tagScores, User userRef) {
        // 1. Tính điểm trung bình (Overall)
        double averageScore = tagScores.values().stream()
                .mapToDouble(d -> d)
                .average()
                .orElse(0.0);

        // 2. Xác định Level tương ứng với số điểm
        Level calculatedLevel = determineLevelByScore(averageScore);

        // 3. Lấy Profile hiện tại (hoặc tạo mới nếu chưa có - Auto-heal)
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseGet(() -> StudentProfile.builder()
                        .user(userRef)
                        .level(Level.UNDETERMINED)
                        .build());

        // 4. Kiểm tra điều kiện thăng hạng
        if (isLevelUp(profile.getLevel(), calculatedLevel)) {
            Level oldLevel = profile.getLevel();
            profile.setLevel(calculatedLevel);
            studentProfileRepository.save(profile);

            log.info("🚀 [LEVEL-UP] User {}: {} -> {}", userId, oldLevel, calculatedLevel);
        } else {
            log.info("🛡️ [LEVEL-PROTECTED] User {} giữ rank {} (Điểm thi: {} ~ {})",
                    userId, profile.getLevel(), averageScore, calculatedLevel);
        }
    }

    /**
     * Quy tắc đổi điểm ra Level (Có thể cấu hình lại dễ dàng)
     */
    public Level determineLevelByScore(double score) {
        if (score < 50.0) return Level.BEGINNER;
        if (score < 80.0) return Level.INTERMEDIATE;
        return Level.ADVANCED;
    }

    /**
     * So sánh trọng số giữa 2 Level
     */
    private boolean isLevelUp(Level current, Level next) {
        return getLevelWeight(next) > getLevelWeight(current);
    }

    /**
     * Định nghĩa thứ tự cao thấp của Rank
     */
    private int getLevelWeight(Level level) {
        if (level == null) return -1;
        return switch (level) {
            case UNDETERMINED -> 0;
            case BEGINNER -> 1;
            case INTERMEDIATE -> 2;
            case ADVANCED -> 3;
        };
    }
}