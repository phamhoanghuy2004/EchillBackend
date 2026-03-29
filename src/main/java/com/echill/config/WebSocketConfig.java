package com.echill.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. Khai báo endpoint (Cổng giao tiếp) để Frontend kết nối vào
        registry.addEndpoint("/ws")
                // 2. Mở CORS để React/Vue chạy port khác (VD: 3000, 5173) vẫn kết nối được
                .setAllowedOriginPatterns("*")
                // 3. Fallback: Nếu mạng công ty/trường học chặn WebSocket thuần, tự động hạ cấp xuống HTTP Long-Polling
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 4. Kênh phát sóng (Server -> Client): Mọi tin nhắn từ server sẽ bắt đầu bằng /topic
        // (Khớp với cái /topic/lessons/ mà bạn dùng ở Service)
        registry.enableSimpleBroker("/topic");

        // 5. Kênh nhận lệnh (Client -> Server): Nếu Frontend muốn gửi tin nhắn lên (VD: chat) thì bắt đầu bằng /app
        registry.setApplicationDestinationPrefixes("/app");
    }
}
