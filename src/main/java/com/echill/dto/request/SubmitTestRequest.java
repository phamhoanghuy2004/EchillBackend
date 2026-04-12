package com.echill.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitTestRequest {
    @NotNull(message = "SESSION_ID_REQUIRED")
    Long sessionId;

    Map<Long, Long> answers;
}
