package com.echill.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SaveVideoDraftRequest {
    @NotBlank(message = "Không được để trống Public ID của Video")
    String publicVideoId;

    @NotBlank(message = "Không được để trống URL gốc của Video")
    String rawUrl;
}
