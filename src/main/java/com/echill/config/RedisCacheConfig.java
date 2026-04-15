package com.echill.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    // 1. Cấu hình tiêu chuẩn (Mặc định cho các @Cacheable không được chỉ định rõ)
    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60)) // Mặc định 60 phút
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // 💥 FIX LỖI: Lắp đạn vào súng! Gọi hàm custom ở dưới lên đây!
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(customJackson2JsonRedisSerializer()));
    }

    // 2. Cấu hình CacheManager để phân chia TTL cho từng "cacheNames"
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {

        // Tạo một Map để chứa cấu hình TTL riêng cho từng cái tên Cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("courseDetail", defaultCacheConfiguration().entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("categories", defaultCacheConfiguration().entryTtl(Duration.ofDays(3)));
        cacheConfigurations.put("testPractice", defaultCacheConfiguration().entryTtl(Duration.ofDays(7)));
        cacheConfigurations.put("testReviewCache", defaultCacheConfiguration().entryTtl(Duration.ofDays(30)));
        cacheConfigurations.put("topStudents", defaultCacheConfiguration().entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("lessonDetails", defaultCacheConfiguration().entryTtl(Duration.ofDays(1)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfiguration()) // Gắn cấu hình mặc định
                .withInitialCacheConfigurations(cacheConfigurations) // Gắn cấu hình riêng lẻ
                .build();
    }

    // 💥 3. HÀM CHẾ TẠO SERIALIZER SIÊU CẤP (Support Method)
    private GenericJackson2JsonRedisSerializer customJackson2JsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Cấp quyền cho Jackson đọc mọi properties (private/public)
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // Ghi kèm Class Type vào JSON để lúc Redis nhả ra nó biết ép kiểu về đúng DTO
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        // Dạy cho Jackson hiểu LocalDateTime (Khắc phục lỗi SerializationException)
        objectMapper.registerModule(new JavaTimeModule());

        // Ép Jackson ghi ngày tháng ra dạng String ISO-8601 ("2026-04-13T19:54:37") cho đẹp
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}