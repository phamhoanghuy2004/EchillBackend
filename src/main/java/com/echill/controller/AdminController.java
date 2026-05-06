package com.echill.controller;

import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.UserResponse;
import com.echill.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admins")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminController {

    UserService userService;

    @GetMapping("/my-profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> getMyProfile() {
        return ApiResponse.<UserResponse>builder()
                .data(userService.getMyProfile())
                .build();
    }
}
