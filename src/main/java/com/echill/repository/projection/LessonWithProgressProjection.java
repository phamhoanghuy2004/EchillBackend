package com.echill.repository.projection;

import com.echill.entity.enums.VideoStatus;

public interface LessonWithProgressProjection {
    Long getLessonId();
    String getTitle();
    Integer getDisplayOrder();
    Long getDurationSeconds();
    VideoStatus getVideoStatus();
    Integer getLessonVersion();

    // Tài nguyên đính kèm (Scalar Subquery)
    Boolean getHasDocument();
    Boolean getHasTest();

    // Thông tin tiến độ (Bảng LessonProgress - CÓ THỂ NULL do LEFT JOIN)
    Long getProgressId();
    Boolean getIsCompleted();
    Integer getVersionCompleted();
    Integer getLastWatchedSecond();
}
