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
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long studentId;
    String name;
    String email;
    String avatar;
    String courseName;
    Long courseId;
    Integer progress;
    @JsonFormat(pattern = "dd/MM/yyyy", timezone = "Asia/Ho_Chi_Minh")
    Instant joinDate;

    public TeacherStudentResponse(Long id, Long studentId, String name, String email, String avatar,
                                  String courseName, Long courseId,
                                  Long completedLessons, Integer totalLessons,
                                  Instant joinDate) {
        this.id = id;
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.avatar = avatar;
        this.courseName = courseName;
        this.courseId = courseId;
        // Tính progress %
        if (totalLessons != null && totalLessons > 0) {
            int raw = (int) ((completedLessons != null ? completedLessons : 0L) * 100 / totalLessons);
            this.progress = Math.min(raw, 100);
        } else {
            this.progress = 0;
        }
        this.joinDate = joinDate;
    }

    // Secondary constructor for int compatibility
    public TeacherStudentResponse(Long id, Long studentId, String name, String email, String avatar,
                                  String courseName, Long courseId,
                                  int completedLessons, Integer totalLessons,
                                  Instant joinDate) {
        this(id, studentId, name, email, avatar, courseName, courseId, (long) completedLessons, totalLessons, joinDate);
    }
}
