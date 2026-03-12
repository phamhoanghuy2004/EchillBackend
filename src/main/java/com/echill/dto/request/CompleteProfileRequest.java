package com.echill.dto.request;

import jakarta.validation.constraints.NotBlank;
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

    LocalDate dob;

    @NotBlank(message = "Job title cannot be blank")
    String jobTitle;
}
