package com.echill.policy;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TestScoringPolicy {
    public static final int GRACE_PERIOD_SECONDS = 60;
    public static final double MAX_SCORE_BASE = 10.0;
    public static final int SCORE_SCALE = 2;
    public static final double PENALTY_SCORE_FOR_LATE = 0.0;
    public static final int MAX_PAYLOAD_SIZE = 200;

    // Kiểm tra nộp trễ có tính dung sai
    public boolean isSubmissionLate(LocalDateTime actualEndTime, LocalDateTime currentTime) {
        LocalDateTime submissionDeadlineWithGrace = actualEndTime.plusSeconds(GRACE_PERIOD_SECONDS);
        return currentTime.isAfter(submissionDeadlineWithGrace);
    }

    // Tính điểm trên thang 10
    public double calculateTotalScore(int correctCount, int totalQuestions) {
        if (totalQuestions == 0) return 0.0;
        double rawScore = ((double) correctCount / totalQuestions) * MAX_SCORE_BASE;
        double scaleFactor = Math.pow(10, SCORE_SCALE);
        return Math.round(rawScore * scaleFactor) / scaleFactor;
    }
}
