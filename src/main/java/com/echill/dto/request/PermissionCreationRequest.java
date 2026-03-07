package com.echill.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionCreationRequest {
    @NotBlank(message = "PERMISSION_NAME_REQUIRED")
    String name;

    String description;
}
