package com.echill.controller;

import com.echill.dto.response.ApiResponse;
import com.echill.service.RedisService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminCacheController {
    
    RedisService redisService;

    @DeleteMapping("/clear-all")
    public ApiResponse<String> clearAllSystemCaches() {
        redisService.clearAllSystemCaches();
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Đã xóa toàn bộ cache hệ thống và session bài thi thành công")
                .data("OK")
                .build();
    }
}
