package com.echill.dto.response.learner;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter // Chỉ cần Getter để Jackson chuyển thành JSON, bỏ @Data cho nhẹ
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProgressStatusResponse {
    Integer currentSecond;
    Boolean isVideoWatched;
    Boolean isQuizPassed;
}
