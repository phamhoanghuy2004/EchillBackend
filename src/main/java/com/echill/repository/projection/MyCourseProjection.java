package com.echill.repository.projection;

import java.time.Instant;

public interface MyCourseProjection {
    Long getEnrollmentId();
    Long getCourseId();
    String getCourseName();
    String getCourseImage();
    Integer getTotalLessons();
    String getTeacherName();
    String getTeacherAvatar();
    Instant getLastAccessedAt();

    // Số lượng bài đếm được từ Subquery
    Long getCompletedLessons();
}
