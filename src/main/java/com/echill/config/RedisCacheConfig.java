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
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60)) // Mặc định 60 phút
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(customJackson2JsonRedisSerializer()));
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("courseDetail", defaultCacheConfiguration().entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("categories", defaultCacheConfiguration().entryTtl(Duration.ofDays(3)));
        cacheConfigurations.put("testPractice", defaultCacheConfiguration().entryTtl(Duration.ofDays(7)));
        cacheConfigurations.put("testReviewCache", defaultCacheConfiguration().entryTtl(Duration.ofDays(30)));
        cacheConfigurations.put("topStudents", defaultCacheConfiguration().entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("lessonDetails", defaultCacheConfiguration().entryTtl(Duration.ofDays(1)));
        cacheConfigurations.put("tags", defaultCacheConfiguration().entryTtl(Duration.ofDays(30)));
        cacheConfigurations.put("allCourses", defaultCacheConfiguration().entryTtl(Duration.ofHours(24))); // Cache danh sách khóa học 24h
        cacheConfigurations.put("allTeachers", defaultCacheConfiguration().entryTtl(Duration.ofHours(24))); // Cache danh sách giáo viên 24h
        cacheConfigurations.put("latestBlogs", defaultCacheConfiguration().entryTtl(Duration.ofHours(24))); // Cache blog mới nhất 24h
        cacheConfigurations.put("testSetDetails", defaultCacheConfiguration().entryTtl(Duration.ofDays(7)));
        cacheConfigurations.put("testQuestionCounts", defaultCacheConfiguration().entryTtl(Duration.ofDays(30)));
        cacheConfigurations.put("testSectionSummaries", defaultCacheConfiguration().entryTtl(Duration.ofDays(7)));
        cacheConfigurations.put("allReviews", defaultCacheConfiguration().entryTtl(Duration.ofHours(24))); // Cache blog mới nhất 24h
        cacheConfigurations.put("getMyReviewByCourse", defaultCacheConfiguration().entryTtl(Duration.ofHours(24))); // Cache blog mới nhất 24h
        cacheConfigurations.put("allReviewsByCourse", defaultCacheConfiguration().entryTtl(Duration.ofHours(24))); // Cache blog mới nhất 24h
        cacheConfigurations.put("featuredReviews", defaultCacheConfiguration().entryTtl(Duration.ofHours(24))); // Cache blog mới nhất 24h

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfiguration())
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private GenericJackson2JsonRedisSerializer customJackson2JsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}