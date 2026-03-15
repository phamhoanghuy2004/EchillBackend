package com.echill.dto.response;

import com.echill.entity.enums.Level;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String username;
    String email;
    String fullName;
    String address;
    LocalDate dob;
    String jobTitle;
    String avatarUrl;
    Long currentCoin;

    // Thông tin từ StudentProfile
    Level level;

    // Thông tin Mục tiêu học tập (Có thể null nếu học viên chưa thiết lập)
    StudyGoalResponse activeGoal;

    Set<String> roles;
}
