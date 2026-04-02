package com.echill.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherProfileUpdateRequest {
    
    @Size(max = 5000, message = "BIO_TOO_LONG")
    String bio;
    
}
