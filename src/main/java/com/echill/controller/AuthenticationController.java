package com.echill.controller;

import com.echill.dto.request.AuthenticationRequest;
import com.echill.dto.request.IntrospectRequest;
import com.echill.dto.request.LogoutRequest;
import com.echill.dto.request.RefreshRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.AuthenticationResponse;
import com.echill.dto.response.IntrospectResponse;
import com.echill.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;


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
    ApiResponse<AuthenticationResponse> authenticate(@Valid @RequestBody RefreshRequest request)
            throws ParseException, JOSEException {
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

}
