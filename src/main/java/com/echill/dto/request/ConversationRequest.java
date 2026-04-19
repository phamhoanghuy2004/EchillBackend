package com.echill.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationRequest {

    @NotNull(message = "teacherId is required")
    Long teacherId;

    @NotNull(message = "studentId is required")
    Long studentId;
}
