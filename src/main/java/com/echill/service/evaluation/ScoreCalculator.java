package com.echill.service.evaluation;

import org.springframework.stereotype.Component;

@Component
public class ScoreCalculator {
    private static final double MAX_SCORE = 10.0;
    private static final int SCALE = 2;
    private static final double LATE_PENALTY_SCORE = 0.0;

    public double calculate(int correctCount, int totalQuestions, boolean isLate) {
        if (isLate) return LATE_PENALTY_SCORE;
        if (totalQuestions == 0) return 0.0;

        double rawScore = ((double) correctCount / totalQuestions) * MAX_SCORE;
        double scaleFactor = Math.pow(10, SCALE);
        return Math.round(rawScore * scaleFactor) / scaleFactor;
    }
}
