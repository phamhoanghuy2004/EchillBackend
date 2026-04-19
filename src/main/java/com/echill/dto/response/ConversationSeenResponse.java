package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationSeenResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long conversationId;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long userId;
    
    Instant lastSeenAt;
}
