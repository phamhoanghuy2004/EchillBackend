package com.echill.config;

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
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

    // 2. Cấu hình CacheManager để phân chia TTL cho từng "cacheNames"
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {

        // Tạo một Map để chứa cấu hình TTL riêng cho từng cái tên Cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 💥 Ghi đè TTL cho cache "courseDetail" -> 1 tiếng
        cacheConfigurations.put("courseDetail", defaultCacheConfiguration().entryTtl(Duration.ofHours(1)));

        // 💥 Ghi đè TTL cho cache "categories" -> 24 tiếng
        cacheConfigurations.put("categories", defaultCacheConfiguration().entryTtl(Duration.ofHours(24)));

        // Nếu sau này có cache "topStudents", cho nó sống 15 phút thôi
        cacheConfigurations.put("topStudents", defaultCacheConfiguration().entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfiguration()) // Gắn cấu hình mặc định
                .withInitialCacheConfigurations(cacheConfigurations) // Gắn cấu hình riêng lẻ
                .build();
    }
}