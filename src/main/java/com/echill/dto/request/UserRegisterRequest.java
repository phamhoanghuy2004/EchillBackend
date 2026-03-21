package com.echill.dto.request;

import com.echill.validator.DobConstraint;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRegisterRequest {

    @NotBlank(message = "USERNAME_REQUIRED")
    @Size(max = 50, message = "USERNAME_TOO_LONG")
    String username;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 6, max = 50, message = "PASSWORD_LENGTH_INVALID")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "PASSWORD_FORMAT_INVALID")
    String password;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID_FORMAT")
    @Size(max = 255, message = "EMAIL_TOO_LONG")
    String email;

    @NotBlank(message = "FULL_NAME_REQUIRED")
    @Size(max = 100, message = "FULL_NAME_TOO_LONG")
    String fullName;

    @Past(message = "DOB_INVALID_PAST")
    @DobConstraint(min = 16, message = "INVALID_DOB")
    LocalDate dob;

    @Size(max = 255, message = "ADDRESS_TOO_LONG")
    String address;

    @NotBlank(message = "JOB_TITLE_REQUIRED")
    @Size(max = 100, message = "JOB_TITLE_TOO_LONG")
    String jobTitle;

    @NotBlank(message = "ROLE_REQUIRED")
    @Pattern(regexp = "^(STUDENT|TEACHER)$", message = "ROLE_INVALID")
    String role;

}
