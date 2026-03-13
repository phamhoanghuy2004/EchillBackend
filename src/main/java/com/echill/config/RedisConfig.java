package com.echill.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfig {
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Tối ưu: Ép kiểu Key và Value thành String chuẩn xác, dễ đọc khi xem trên Redis Insight
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());

        return template;
    }

    @Bean
    public ApplicationRunner checkRedisConnection(RedisTemplate<String, String> redisTemplate) {
        return args -> {
            try {
                // Gửi lệnh PING đến Redis Server
                String pingResponse = redisTemplate.getConnectionFactory().getConnection().ping();
                if ("PONG".equalsIgnoreCase(pingResponse)) {
                    log.info("✅ Kết nối Redis Server thành công!");
                }
            } catch (Exception e) {
                log.error("❌ Kết nối Redis thất bại. Vui lòng kiểm tra lại cấu hình: {}", e.getMessage());
            }
        };
    }
}
