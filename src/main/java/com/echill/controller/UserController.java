package com.echill.controller;

import com.echill.dto.request.CompleteProfileRequest;
import com.echill.dto.request.UserUpdateRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @PutMapping("/complete-profile")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ApiResponse<Void> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        userService.completeProfile(request);
        return ApiResponse.<Void>builder()
                .message("Profile updated successfully")
                .build();
    }

    @PutMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ApiResponse<Void> update (@Valid @RequestPart("data") UserUpdateRequest request,
                                     @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        userService.update(request, avatar);
        return ApiResponse.<Void>builder()
                .message("Profile updated successfully")
                .build();
    }
}
