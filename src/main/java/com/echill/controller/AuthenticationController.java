package com.echill.controller;

import com.echill.dto.request.*;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.AuthenticationResponse;
import com.echill.dto.response.IntrospectResponse;
import com.echill.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest authenticationRequest) {
        return ApiResponse.<AuthenticationResponse>builder()
                .data(authenticationService.authenticate(authenticationRequest))
                .build();
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@Valid @RequestBody IntrospectRequest introspectRequest) {
        return ApiResponse.<IntrospectResponse>builder()
                .data(authenticationService.introspect(introspectRequest))
                .build();
    }

    @PostMapping("/google-login")
    public ApiResponse<AuthenticationResponse> googleLogin(@Valid @RequestBody com.echill.dto.request.GoogleLoginRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .data(authenticationService.googleLogin(request))
                .build();
    }

    @PostMapping("/refresh")
    ApiResponse<AuthenticationResponse> authenticate(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .data(authenticationService.refreshToken(request))
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest logoutRequest) {
        authenticationService.logout(logoutRequest);
        return ApiResponse.<Void>builder()
                .build();
    }

    @PostMapping(value = "/register", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> register(@Valid @RequestPart("data") UserRegisterRequest request,
                                      @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        authenticationService.register(request, avatar);
        return ApiResponse.<Void>builder()
                .message("User registered successfully!")
                .build();
    }

    @PostMapping("/verify-register-otp")
    public ApiResponse<AuthenticationResponse> verity(@Valid @RequestBody VerifyOtpRequest verifyOtpRequest) {
        return ApiResponse.<AuthenticationResponse>builder()
                .data(authenticationService.verifyRegisterOtp(verifyOtpRequest))
                .build();
    }

    @PostMapping("/resend-register-otp")
    public ApiResponse<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authenticationService.resendOtp(request.getEmail(), false);
        return ApiResponse.<Void>builder()
                .message("Đã gửi lại mã OTP. Vui lòng kiểm tra hộp thư của bạn.")
                .build();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        authenticationService.forgotPassword(request);
        return ApiResponse.<Void>builder()
                .message("Đã gửi mã OTP khôi phục mật khẩu. Vui lòng kiểm tra email!")
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .message("Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.")
                .build();
    }

}
