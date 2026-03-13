package com.echill.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // Luôn duy trì 2 luồng chực chờ gửi mail
        executor.setMaxPoolSize(5);  // Tối đa 5 luồng hoạt động cùng lúc khi hệ thống tải nặng
        executor.setQueueCapacity(50); // Nếu > 5 người đăng ký cùng lúc, đưa 50 người tiếp theo vào hàng đợi
        executor.setThreadNamePrefix("EmailSender-"); // Đặt tên để dễ debug trong Log
        executor.initialize();
        return executor;
    }
}
