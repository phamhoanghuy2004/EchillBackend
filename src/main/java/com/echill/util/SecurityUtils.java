package com.echill.util;

import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@UtilityClass
@Slf4j
public class SecurityUtils {
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 💥 Bọc thép: Kiểm tra xem có đăng nhập không và Token có chuẩn JWT không
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            log.warn("Không tìm thấy thông tin xác thực hợp lệ trong Security Context");
            throw new AppException(ErrorEnum.UNAUTHENTICATED);
        }

        Number userIdNumber = jwt.getClaim("userId");
        if (userIdNumber == null) {
            log.error("Token không chứa claim 'userId'");
            throw new AppException(ErrorEnum.UNAUTHENTICATED);
        }

        return userIdNumber.longValue();
    }

    /**
     * Lấy Username (Email) của user đang đăng nhập
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorEnum.UNAUTHENTICATED);
        }
        return authentication.getName();
    }

    public static void validateOwnership(Long ownerId) {
        Long currentUserId = getCurrentUserId();
        if (!ownerId.equals(currentUserId)) {
            throw new AppException(ErrorEnum.UNAUTHORIZED); // Mã lỗi 403 Forbidden
        }
    }
}
