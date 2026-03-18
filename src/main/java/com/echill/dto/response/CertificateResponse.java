package com.echill.dto.response;

import com.echill.entity.enums.CertType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class CertificateResponse {
    Long id;
    CertType certType;
    Double totalScore;
    Double listeningScore;
    Double readingScore;
    Double speakingScore;
    Double writingScore;
    LocalDate issuedDate;
    String evidenceUrl;
}
