package com.echill.controller;

import com.echill.dto.request.CoinPackageCreateRequest;
import com.echill.dto.request.CoinPackagePageRequest;
import com.echill.dto.request.CoinPackageUpdateRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.CoinPackageResponse;
import com.echill.dto.response.PageResponse;
import com.echill.service.CoinPackageService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/coin-packages")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CoinPackageController {

    CoinPackageService coinPackageService;

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping
    public ApiResponse<CoinPackageResponse> createCoinPackage(
            @Valid @RequestBody CoinPackageCreateRequest request) {

        CoinPackageResponse response = coinPackageService.createCoinPackage(request);

        return ApiResponse.<CoinPackageResponse>builder()
                .message("Tạo gói xu thành công")
                .data(response)
                .build();
    }

    @GetMapping("/public")
    public ApiResponse<PageResponse<CoinPackageResponse>> getActiveCoinPackages(
            @Valid CoinPackagePageRequest request) {

        PageResponse<CoinPackageResponse> response = coinPackageService.getActiveCoinPackages(request);

        return ApiResponse.<PageResponse<CoinPackageResponse>>builder()
                .message("Lấy danh sách gói xu thành công")
                .data(response)
                .build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping
    public ApiResponse<PageResponse<CoinPackageResponse>> getAllCoinPackages(
            @Valid CoinPackagePageRequest request) {

        return ApiResponse.<PageResponse<CoinPackageResponse>>builder()
                .message("Lấy tất cả danh sách gói xu thành công")
                .data(coinPackageService.getAllCoinPackages(request))
                .build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/{id}")
    public ApiResponse<CoinPackageResponse> updateCoinPackage(
            @PathVariable Long id,
            @Valid @RequestBody CoinPackageUpdateRequest request) {

        return ApiResponse.<CoinPackageResponse>builder()
                .message("Cập nhật gói xu thành công")
                .data(coinPackageService.updateCoinPackage(id, request))
                .build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/{id}")
    public ApiResponse<CoinPackageResponse> getCoinPackageById(@PathVariable Long id) {
        return ApiResponse.<CoinPackageResponse>builder()
                .message("Lấy thông tin gói xu thành công")
                .data(coinPackageService.getCoinPackageById(id))
                .build();
    }
}
