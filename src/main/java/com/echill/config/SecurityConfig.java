package com.echill.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

    static String[] PUBLIC_POST_ENDPOINTS = {
            "/users", "/auth/login", "/auth/introspect", "/auth/logout", "/auth/register", "/auth/verify-register-otp",
            "/auth/resend-register-otp", "/auth/google-login", "/auth/forgot-password", "/auth/reset-password",
            "/auth/refresh", "/webhook/cloudinary", "/ws/**", "/consultations/submit"
    };

    static String[] PUBLIC_GET_ENDPOINTS = {
            "/courses", "/courses/**", "/exams", "/exams/**", "/ws/**", "/categories", "/categories/**", "/payments/vnpay-ipn",
            "/teachers/all", "/teachers/random", "/blogs", "/blogs/**", "/reviews/featured", "/reviews/course/**", "/certificates/top-toeic",
            "/tags", "/tags/**", "/test-sets", "/test-sets/**"
    };

    CustomJwtDecoder customJwtDecoder;

    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

        // Cấu hình phân quyền Endpoint
        httpSecurity.authorizeHttpRequests(request ->
                request.requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                        .anyRequest().authenticated());

        // Cấu hình JWT Decoder & Xử lý ngoại lệ 401
        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer -> jwtConfigurer
                        .decoder(customJwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
        );

        // Gom chung xử lý ngoại lệ 401 và 403 vào khối exceptionHandling của Spring Security
        httpSecurity.exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
        );

        // Tắt CSRF (Bắt buộc cho API RESTful dùng JWT)
        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        // Bật tính năng CORS trong Filter Chain
        httpSecurity.cors(Customizer.withDefaults());

        // =========================================================
        // Cấu hình STATELESS - Tắt hoàn toàn Session
        // Giúp Server tiết kiệm tối đa RAM khi có hàng ngàn user truy cập
        // =========================================================
        httpSecurity.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return httpSecurity.build();
    }

    // =========================================================
    // CẤU HÌNH CORS CHUẨN CHO SPRING SECURITY ĐỨNG SAU NGINX
    // =========================================================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        // Thay "http://localhost:3000" bằng domain frontend của bạn sau khi deploy
        corsConfiguration.addAllowedOrigin("http://localhost:5173");
        corsConfiguration.addAllowedOrigin("https://front-end-online-e-course.vercel.app"); // Cho phép FE trên Vercel
        corsConfiguration.addAllowedMethod("*"); // Cho phép mọi method (GET, POST, PUT, DELETE, OPTIONS)
        corsConfiguration.addAllowedHeader("*"); // Cho phép mọi header (Authorization, Content-Type,...)
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        var jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}