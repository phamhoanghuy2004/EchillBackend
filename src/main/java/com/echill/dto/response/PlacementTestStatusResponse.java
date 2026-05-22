package com.echill.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlacementTestStatusResponse {
    boolean hasCompleted;
    String currentLevel;
}
