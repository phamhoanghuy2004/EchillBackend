package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
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
    String status;
    Set<RoleResponse> roles;
}
