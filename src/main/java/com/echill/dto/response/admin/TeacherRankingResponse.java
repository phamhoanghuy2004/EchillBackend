package com.echill.dto.response.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherRankingResponse {
    int rank;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long teacherId;
    String teacherName;
    String teacherAvatar;
    BigDecimal revenue;
    Long salesCount;
}
