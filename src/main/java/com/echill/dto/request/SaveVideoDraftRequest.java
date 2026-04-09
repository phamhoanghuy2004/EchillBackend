package com.echill.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotNull(message = "Không được để trống thời lượng video")
    @Positive(message = "Thời lượng video phải lớn hơn 0 giây")
    Long durationSeconds;
}
