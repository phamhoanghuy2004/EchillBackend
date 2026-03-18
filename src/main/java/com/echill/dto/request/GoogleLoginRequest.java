package com.echill.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GoogleLoginRequest {
    @NotBlank(message = "Google credential token cannot be blank")
    String credential;

    @NotBlank(message = "ROLE_REQUIRED")
    @Pattern(regexp = "^(STUDENT|TEACHER)$", message = "ROLE_INVALID")
    String role;
}
