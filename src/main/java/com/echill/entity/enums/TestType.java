package com.echill.entity.enums;

import java.util.Arrays;
import java.util.List;

public enum TestType {
    TOEIC,
    PRACTICE,
    PLACEMENT_TEST,
    TOEIC;

    // 🔴 BÍ QUYẾT SENIOR: Hàm gom nhóm các loại đề được phép hiển thị
    public static List<String> getPracticePageTypes() {
        return Arrays.stream(values()) // Lấy toàn bộ [PRACTICE, PLACEMENT_TEST, TOEIC, IELTS...]
                // Lọc bỏ đi 2 thằng nội bộ
                .filter(type -> type != PRACTICE && type != PLACEMENT_TEST)
                // Biến đổi từ Enum sang String
                .map(Enum::name)
                .toList(); // Đóng gói thành List<String> (Dùng .collect(Collectors.toList()) nếu dùng Java cũ)
    }
}
