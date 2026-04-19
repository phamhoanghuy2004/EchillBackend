package com.echill.config;

import com.echill.config.CustomJwtDecoder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.Collection;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CustomJwtDecoder customJwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            Jwt jwt = customJwtDecoder.decode(token);
                            Authentication authentication = jwtAuthenticationConverter.convert(jwt);
                            
                            // Localized Fix: Wrap the authentication to use userId as the Principal name
                            // This ensures convertAndSendToUser(userId) matches correctly.
                            if (jwt.hasClaim("userId")) {
                                String userId = jwt.getClaim("userId").toString();
                                accessor.setUser(new StompAuthentication(authentication, userId));
                            } else {
                                accessor.setUser(authentication);
                            }
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Invalid token");
                        }
                    }
                }
                return message;
            }
        });
    }

    /**
     * A localized Authentication wrapper that reports the userId as the name.
     * This is used only for STOMP user destination routing.
     */
    private static class StompAuthentication implements Authentication {
        private final Authentication delegate;
        private final String userId;

        public StompAuthentication(Authentication delegate, String userId) {
            this.delegate = delegate;
            this.userId = userId;
        }

        @Override public String getName() { return userId; }
        @Override public Collection<? extends GrantedAuthority> getAuthorities() { return delegate.getAuthorities(); }
        @Override public Object getCredentials() { return delegate.getCredentials(); }
        @Override public Object getDetails() { return delegate.getDetails(); }
        @Override public Object getPrincipal() { return delegate.getPrincipal(); }
        @Override public boolean isAuthenticated() { return delegate.isAuthenticated(); }
        @Override public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException { 
            delegate.setAuthenticated(isAuthenticated); 
        }
    }
}
