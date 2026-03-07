package com.echill.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationRequest {
    @NotBlank(message = "USERNAME_REQUIRED")
    @Size(min = 4, message = "INVALID_USERNAME")
    String username;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 5, message = "INVALID_PASSWORD")
    String password;
}
