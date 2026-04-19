package com.echill.dto.response;

import com.echill.entity.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long conversationId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long senderId;
    String senderName;
    String senderAvatar;
    String content;
    MessageType messageType;
    Instant sentAt;
    Boolean isDeleted;
}
