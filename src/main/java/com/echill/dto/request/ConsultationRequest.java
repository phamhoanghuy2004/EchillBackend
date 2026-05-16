package com.echill.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Year;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConsultationRequest {
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^(0|\\+84)[0-9]{9,10}$",
            message = "Invalid Vietnamese phone number"
    )
    String phoneNumber;

    @Min(value = 1900, message = "Birth year must be greater than or equal to 1900")
    @Max(value = Year.MAX_VALUE, message = "Invalid birth year")
    Integer birthYear;

    @Size(max = 500, message = "Topic must not exceed 500 characters")
    String topic;
}
