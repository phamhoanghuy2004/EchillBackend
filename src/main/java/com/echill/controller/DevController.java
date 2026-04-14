package com.echill.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
public class DevController {
    private final RedisConnectionFactory redisConnectionFactory;

    // 💥 API "Nút đỏ": Quét sạch toàn bộ Redis Cache
    @GetMapping("/flush-redis")
    public String flushRedis() {
        // Lấy connection hiện tại và ra lệnh FLUSHALL
        redisConnectionFactory.getConnection().serverCommands().flushAll();
        return "🔥 Đã dọn sạch sành sanh Redis trên Aiven! Chủ tịch F5 lại API đi!";
    }
}
