package com.echill.entity.enums;

import lombok.Getter;

@Getter
public enum TagGroup {
    ENGLISH_TOEIC("Chứng chỉ TOEIC"),
    ENGLISH_IELTS("Chứng chỉ IELTS"),
    ENGLISH_COMMUNICATION("Tiếng Anh Giao Tiếp"),

    IT_BACKEND("Lập trình Backend"),
    IT_FRONTEND("Lập trình Frontend"),
    IT_GENERAL("Kiến thức IT Nền tảng"),

    SOFT_SKILLS("Kỹ năng mềm");

    private final String displayName;

    TagGroup(String displayName) {
        this.displayName = displayName;
    }
}
