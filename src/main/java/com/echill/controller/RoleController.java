package com.echill.controller;

import com.echill.dto.request.RoleCreationRequest;
import com.echill.dto.request.RoleUpdateRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.RoleResponse;
import com.echill.service.RoleService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleController {
    RoleService roleService;

    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RoleResponse> create (@Valid @RequestBody RoleCreationRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .data(roleService.create(request))
                .build();
    }

    @PutMapping("/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RoleResponse> update(
            @Valid @RequestBody RoleUpdateRequest request,
            @PathVariable("roleName") String roleName // CHỈ ĐỊNH RÕ TÊN BIẾN Ở ĐÂY
    ) {
        return ApiResponse.<RoleResponse>builder()
                .data(roleService.update(roleName, request))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<RoleResponse>> getAll() {
        return ApiResponse.<List<RoleResponse>>builder()
                .data(roleService.getAll())
                .build();
    }

    @DeleteMapping("/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable("roleName") String roleName) {
        roleService.delete(roleName);
        return ApiResponse.<Void>builder()
                .message("Xóa Role thành công")
                .build();
    }

    @GetMapping("/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RoleResponse> getRoleByName(@PathVariable("roleName") String roleName) {
        return ApiResponse.<RoleResponse>builder()
                .data(roleService.getByName(roleName))
                .build();
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<RoleResponse>> getRolesByUser(@PathVariable("userId") Long userId) {
        return ApiResponse.<List<RoleResponse>>builder()
                .data(roleService.getRolesByUserId(userId))
                .build();
    }
}
