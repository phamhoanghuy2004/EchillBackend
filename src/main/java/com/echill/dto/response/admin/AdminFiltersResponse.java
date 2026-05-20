package com.echill.dto.response.admin;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminFiltersResponse {
    List<Map<String, Object>> teachers;
    List<Map<String, Object>> courses;
}
