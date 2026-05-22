package com.echill.controller;

import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.UserResponse;
import com.echill.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import com.echill.dto.request.AdminUserSearchRequest;
import com.echill.dto.response.PageResponse;
import com.echill.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admins")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminController {

    UserService userService;
    AdminUserService adminUserService;

    @GetMapping("/my-profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> getMyProfile() {
        return ApiResponse.<UserResponse>builder()
                .data(userService.getMyProfile())
                .build();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> getUsers(@Valid @ModelAttribute AdminUserSearchRequest request) {
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .data(adminUserService.getUsers(request))
                .build();
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> getUserDetail(@PathVariable Long id) {
        return ApiResponse.<UserResponse>builder()
                .data(adminUserService.getUserDetail(id))
                .build();
    }

    @PatchMapping("/users/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> blockUser(@PathVariable Long id) {
        return ApiResponse.<UserResponse>builder()
                .data(adminUserService.blockUser(id))
                .build();
    }

    @PatchMapping("/users/{id}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> unblockUser(@PathVariable Long id) {
        return ApiResponse.<UserResponse>builder()
                .data(adminUserService.unblockUser(id))
                .build();
    }
}
