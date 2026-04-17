package com.echill.dto.request.leaner;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SyncProgressRequest {
    @NotNull(message = "Số giây hiện tại không được để trống")
    @Min(value = 0, message = "Số giây không hợp lệ")
    Integer currentSecond;

    @Positive(message = "Tốc độ phát video phải lớn hơn 0")
    Double playbackSpeed; // Nhận tốc độ xem từ Frontend (VD: 1.0, 1.25, 2.0)
}
