package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    Double rating;
    String content;
    Long courseId;
    Long userId;
    String userName;
    String userAvatar;
    Instant createdAt;
}
