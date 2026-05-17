package com.echill.mapper;

import com.echill.dto.request.CertificateRequest;
import com.echill.dto.response.CertificateResponse;
import com.echill.entity.Certificate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CertificateMapper {
    CertificateResponse toCertificateResponse(Certificate certificate);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "evidenceUrl", ignore = true)
    Certificate toCertificate(CertificateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "evidenceUrl", ignore = true)
    void updateCertificate(@MappingTarget Certificate certificate, CertificateRequest request);
}
