package com.echill.controller;

import com.echill.dto.request.VoucherCreationRequest;
import com.echill.dto.request.VoucherPageRequest;
import com.echill.dto.request.VoucherUpdateRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.PageResponse;
import com.echill.dto.response.VoucherResponse;
import com.echill.service.VoucherService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoucherController {
    VoucherService voucherService;

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping
    public ApiResponse<VoucherResponse> createVoucher(@Valid @RequestBody VoucherCreationRequest request) {
        VoucherResponse response = voucherService.createVoucher(request);
        return ApiResponse.<VoucherResponse>builder()
                .data(response)
                .build();
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<VoucherResponse> updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody VoucherUpdateRequest request) {

        VoucherResponse response = voucherService.updateVoucher(id, request);
        return ApiResponse.<VoucherResponse>builder()
                .message("Cập nhật mã giảm giá thành công")
                .data(response)
                .build();
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/my-vouchers")
    public ApiResponse<PageResponse<VoucherResponse>> getMyVouchers(
            @Valid VoucherPageRequest request) {

        PageResponse<VoucherResponse> response = voucherService.getMyVouchers(request);

        return ApiResponse.<PageResponse<VoucherResponse>>builder()
                .message("Lấy danh sách Voucher thành công")
                .data(response)
                .build();
    }

    @GetMapping("/public")
    public ApiResponse<List<VoucherResponse>> getPublicVouchers() {
        return ApiResponse.<List<VoucherResponse>>builder()
                .message("Lấy danh sách Voucher Public thành công")
                .data(voucherService.getPublicVouchers())
                .build();
    }

    @GetMapping("/public/combo")
    public ApiResponse<List<VoucherResponse>> getActiveComboVouchers() {
        return ApiResponse.<List<VoucherResponse>>builder()
                .message("Lấy danh sách Voucher Combo thành công")
                .data(voucherService.getActiveComboVouchers())
                .build();
    }
}
