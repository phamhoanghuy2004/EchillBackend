package com.echill.dto.request;

import com.echill.entity.enums.CertType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificateRequest {
    @NotNull(message = "CERT_TYPE_REQUIRED")
    CertType certType;

    Double totalScore;
    Double listeningScore;
    Double readingScore;
    Double speakingScore;
    Double writingScore;

    @NotNull(message = "ISSUED_DATE_REQUIRED")
    LocalDate issuedDate;
}
