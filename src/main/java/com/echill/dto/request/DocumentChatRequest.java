package com.echill.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentChatRequest {
    @NotBlank(message = "Câu hỏi không được để trống")
    String question;
}
