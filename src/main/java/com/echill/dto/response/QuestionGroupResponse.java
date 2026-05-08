package com.echill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionGroupResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;
    String importCode;
    String sharedContent;
    String sharedAudioUrl;
    String sharedImageUrl;
    List<QuestionResponse> questions;
}