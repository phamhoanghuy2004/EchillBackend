package com.echill.dto.request;

import com.echill.entity.enums.TestType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestRequest {
    @NotBlank(message = "TEST_TITLE_REQUIRED")
    String title;

    @NotNull(message = "TEST_TYPE_REQUIRED")
    TestType type;

    @NotNull(message = "DURATION_REQUIRED")
    @Min(value = 1, message = "INVALID_DURATION")
    Integer durationMinutes;

    @NotNull(message = "PASS_SCORE_REQUIRED")
    @Min(value = 0, message = "INVALID_PASS_SCORE")
    @Max(value = 100, message = "INVALID_PASS_SCORE")
    Double passScore;

    @NotNull(message = "TEST_SET_ID_REQUIRED")
    Long testSetId;
}
