package com.echill.dto.response.learner;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter // Chỉ cần Getter để Jackson chuyển thành JSON, bỏ @Data cho nhẹ
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestSetShortcutDto {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String title;
}
