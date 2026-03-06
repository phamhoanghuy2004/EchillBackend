package com.echill.config;

import com.echill.dto.response.ApiResponse;
import com.echill.exception.ErrorEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    // TỐI ƯU 1: Khởi tạo ObjectMapper đúng 1 lần duy nhất và tái sử dụng cho mọi Request
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        ErrorEnum errorEnum = ErrorEnum.UNAUTHENTICATED;

        response.setStatus(errorEnum.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // TỐI ƯU 2: Đảm bảo dữ liệu trả về không bị lỗi font tiếng Việt
        response.setCharacterEncoding("UTF-8");

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorEnum.getCode())
                .message(errorEnum.getMessage())
                .build();

        // Sử dụng cái máy dịch objectMapper đã được "làm nóng" sẵn
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        response.flushBuffer();
    }
}
