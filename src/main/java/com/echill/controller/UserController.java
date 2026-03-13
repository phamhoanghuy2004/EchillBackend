package com.echill.controller;

import com.echill.dto.request.CompleteProfileRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.UserResponse;
import com.echill.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @PutMapping("/complete-profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        userService.completeProfile(request);
        return ApiResponse.<Void>builder()
                .message("Profile updated successfully")
                .build();
    }

    @GetMapping("/my-info")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .data(userService.getMyInfo())
                .build();
    }
}
