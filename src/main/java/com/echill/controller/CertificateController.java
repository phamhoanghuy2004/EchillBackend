package com.echill.controller;

import com.echill.dto.request.CertificateRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.CertificateResponse;
import com.echill.service.CertificateService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/certificates")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CertificateController {
    CertificateService certificateService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<List<CertificateResponse>> getMyCertificates() {
        return ApiResponse.<List<CertificateResponse>>builder()
                .data(certificateService.getMyCertificates())
                .build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<CertificateResponse> createCertificate(
            @Valid @RequestPart("data") CertificateRequest request,
            @RequestPart("evidence") MultipartFile evidence) {
        return ApiResponse.<CertificateResponse>builder()
                .data(certificateService.createCertificate(request, evidence))
                .build();
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<CertificateResponse> updateCertificate(
            @PathVariable Long id,
            @Valid @RequestPart("data") CertificateRequest request,
            @RequestPart(value = "evidence", required = false) MultipartFile evidence) {
        return ApiResponse.<CertificateResponse>builder()
                .data(certificateService.updateCertificate(id, request, evidence))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> deleteCertificate(@PathVariable Long id) {
        certificateService.deleteCertificate(id);
        return ApiResponse.<Void>builder()
                .message("Certificate deleted successfully")
                .build();
    }
}
