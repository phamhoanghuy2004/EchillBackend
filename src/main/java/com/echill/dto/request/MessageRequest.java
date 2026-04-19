package com.echill.dto.request;

import com.echill.entity.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageRequest {

    @NotNull(message = "conversationId is required")
    Long conversationId;

    @NotBlank(message = "content cannot be blank")
    String content;

    @Builder.Default
    MessageType messageType = MessageType.TEXT;
}
