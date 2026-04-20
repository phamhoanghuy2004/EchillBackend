package com.echill.dto.request;

import com.echill.entity.enums.TagGroup;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkillInsightRequest {
    @NotNull(message = "Nhóm kỹ năng (group) không được để trống")
    TagGroup group;
}
