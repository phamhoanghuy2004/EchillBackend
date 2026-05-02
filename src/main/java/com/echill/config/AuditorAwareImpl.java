package com.echill.config;

import com.echill.util.SecurityUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorProvider")
public class AuditorAwareImpl implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check khách vãng lai hoặc hệ thống tự chạy
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {

            // 💥 Mặc định cho hệ thống tự tạo (Bạn có thể quy ước ID = 0L là của SYSTEM)
            return Optional.of(0L);
        }

        try {
            // 💥 Dùng ngay hàng nhà làm của Chủ tịch
            Long currentUserId = SecurityUtils.getCurrentUserId();
            return Optional.of(currentUserId);
        } catch (Exception e) {
            // Nếu có lỗi (VD: token hỏng) thì an toàn nhất là trả về SYSTEM ID
            return Optional.of(0L);
        }
    }
}
