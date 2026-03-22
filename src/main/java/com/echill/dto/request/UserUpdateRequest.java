package com.echill.dto.request;

import com.echill.validator.DobConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {

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

}
