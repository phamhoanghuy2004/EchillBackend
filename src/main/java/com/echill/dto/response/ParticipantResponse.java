package com.echill.dto.response;

import com.echill.entity.enums.ParticipantRole;
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
public class ParticipantResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long userId;
    String fullName;
    String avatarUrl;
    ParticipantRole role;
    Instant lastSeenAt;
}
