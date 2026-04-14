package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherStudentResponse {
    Long id;
    String name;
    String email;
    String avatar;
    String courseName;
    Long courseId;
    Integer progress;
    @JsonFormat(pattern = "dd/MM/yyyy", timezone = "Asia/Ho_Chi_Minh")
    Instant joinDate;

    // ✅ Constructor cho JPQL DTO Projection — DB xử lý, không cần stream().map()
    public TeacherStudentResponse(Long id, String name, String email, String avatar,
                                  String courseName, Long courseId,
                                  Integer completedLessons, Integer totalLessons,
                                  Instant joinDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.avatar = avatar;
        this.courseName = courseName;
        this.courseId = courseId;
        // Tính progress % ngay tại đây, tránh @Transient method
        if (totalLessons != null && totalLessons > 0) {
            int raw = (completedLessons != null ? completedLessons : 0) * 100 / totalLessons;
            this.progress = Math.min(raw, 100);
        } else {
            this.progress = 0;
        }
        this.joinDate = joinDate;
    }
}
