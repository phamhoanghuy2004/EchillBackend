package com.echill.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewResponse {
    Long id;
    Double rating;
    String content;
    Long courseId;
    Long userId;
    String userName;
    String userAvatar;
    Instant createdAt;
}
