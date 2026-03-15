package com.echill.dto.request;

import com.echill.validator.DobConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompleteProfileRequest {
    @NotBlank(message = "Address cannot be blank")
    String address;

    @Past(message = "DOB_INVALID_PAST")
    @DobConstraint(min = 16, message = "INVALID_DOB")
    LocalDate dob;

    @NotBlank(message = "Job title cannot be blank")
    String jobTitle;
}
