package com.echill.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestSetUpdateRequest {
    @NotBlank(message = "TITLE_REQUIRED")
    String title;

    @NotBlank(message = "DESCRIPTION_REQUIRED")
    String description;

    Boolean isPublic;

    Integer year;
}
