package com.echill.controller;

import com.echill.dto.request.PermissionCreationRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.PermissionResponse;
import com.echill.service.PermissionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionController {
    PermissionService permissionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PermissionResponse> create(@Valid @RequestBody PermissionCreationRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .data(permissionService.create(request))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<PermissionResponse>> getAll() {
        return ApiResponse.<List<PermissionResponse>>builder()
                .data(permissionService.getAll())
                .build();
    }


    @GetMapping("/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<PermissionResponse>> getAllByRoleId(@PathVariable Long roleId) {
        return ApiResponse.<List<PermissionResponse>>builder()
                .data(permissionService.getAllByRoleId(roleId))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.<Void>builder()
                .build();
    }
}
