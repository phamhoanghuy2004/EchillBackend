package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherResponse {
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

    // Các trường đặc thù của Teacher
    String bio;
    List<CertificateResponse> certificates; // Teacher có thể có nhiều bằng cấp

    Set<String> roles;
}
