package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String name;
    Boolean isGroup;
    Instant lastMessageAt;
    String lastMessageContent;
    Long unreadCount;
    List<ParticipantResponse> participants;
}
