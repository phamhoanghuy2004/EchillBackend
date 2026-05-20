package com.echill.dto.response;

import com.echill.entity.enums.TagGroup;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TagResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    String name;

    TagGroup tagGroup;

    String groupDisplayName;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long parentId;

    String parentName;
}
