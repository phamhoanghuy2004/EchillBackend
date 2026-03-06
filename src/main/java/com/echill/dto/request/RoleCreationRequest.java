package com.echill.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;


@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleCreationRequest {
    String name;
    String description;
    List<String> permissions;
}
