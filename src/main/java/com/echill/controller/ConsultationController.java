package com.echill.controller;

import com.echill.dto.request.ConsultationRequest;
import com.echill.dto.request.ConsultationSearchRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.ConsultationResponse;
import com.echill.dto.response.PageResponse;
import com.echill.service.ConsultationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consultations")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsultationController {
    ConsultationService service;

    @PostMapping("/submit")
    public ApiResponse<Void> submitRequest(@Valid @RequestBody ConsultationRequest request) {
        service.createRequest(request);
        return ApiResponse.<Void>builder().message("Yêu cầu của bạn đã được gửi đi!").build();
    }

    @PatchMapping("/{id}/claim")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ConsultationResponse> claim(@PathVariable Long id) {
        return ApiResponse.<ConsultationResponse>builder().data(service.claimRequest(id)).build();
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ConsultationResponse> complete(@PathVariable Long id) {
        return ApiResponse.<ConsultationResponse>builder().data(service.markAsCompleted(id)).build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<ConsultationResponse>> getAll(
            @Valid @ModelAttribute ConsultationSearchRequest request) {
        return ApiResponse.<PageResponse<ConsultationResponse>>builder()
                .data(service.getConsultations(request))
                .build();
    }
}
