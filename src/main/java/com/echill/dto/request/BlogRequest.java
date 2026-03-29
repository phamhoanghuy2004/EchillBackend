package com.echill.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogRequest {
    @NotBlank(message = "BLOG_TITLE_REQUIRED")
    String title;

    @NotBlank(message = "BLOG_CONTENT_REQUIRED")
    String content;
}
