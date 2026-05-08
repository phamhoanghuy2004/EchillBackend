package com.echill.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudyTimePingRequest {
    @NotNull(message = "Số giây học không được để trống")
    @Min(value = 1, message = "Số giây học phải lớn hơn 0")
    @Max(value = 300, message = "Cảnh báo gian lận: Số giây một lần ping không được vượt quá 300s (5 phút)")
    Long addedSeconds;
}
