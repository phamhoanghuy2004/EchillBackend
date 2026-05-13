package com.echill.dto.response.learner;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VideoCompleteResponse {
    boolean videoWatched;     // Đã xong video chưa
    boolean lessonCompleted;
}
