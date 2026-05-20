package com.echill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveTestSession { // Bỏ implements Serializable đi vì ta xài JSON
    private Long userId;

    private Long currentParentTagId;
    private int currentLevel = 3;
    private int highestPassedLevel = 0;
    private int questionsAskedCount = 0;
    private int consecutiveFailsAtLevel1 = 0;

    // 🔥 VÁ LỖ HỔNG 2 & 3: Tracking ID câu hỏi
    private Long currentQuestionId;      // Câu hỏi user ĐANG PHẢI LÀM
    private Long lastAnsweredQuestionId; // Câu hỏi user VỪA NỘP XONG (Idempotency)

    // 🟢 FIX BUG 3: KHÔNG DÙNG @Builder.Default, khởi tạo thẳng để Jackson không bị Null
    private LinkedList<Long> pendingParentTagIds = new LinkedList<>();
    private Set<Long> askedQuestionIds = new HashSet<>();
    private Map<Long, Integer> testedChildTags = new HashMap<>();

    // Các hàm Helper tự động khởi tạo nếu Jackson lỡ deserialize ra null
    public LinkedList<Long> getPendingParentTagIds() {
        if (pendingParentTagIds == null) pendingParentTagIds = new LinkedList<>();
        return pendingParentTagIds;
    }

    public Set<Long> getAskedQuestionIds() {
        if (askedQuestionIds == null) askedQuestionIds = new HashSet<>();
        return askedQuestionIds;
    }

    public Map<Long, Integer> getTestedChildTags() {
        if (testedChildTags == null) testedChildTags = new HashMap<>();
        return testedChildTags;
    }

    public void addAskedQuestion(Long questionId) {
        this.getAskedQuestionIds().add(questionId);
        this.questionsAskedCount++;
    }

    public void resetForNewParentTag(Long newParentTagId) {
        this.currentParentTagId = newParentTagId;
        this.currentLevel = 3;
        this.highestPassedLevel = 0;
        this.questionsAskedCount = 0;
        this.consecutiveFailsAtLevel1 = 0;
        this.getTestedChildTags().clear();

        // Reset luôn State câu hỏi khi qua nhánh mới
        this.currentQuestionId = null;
    }

}