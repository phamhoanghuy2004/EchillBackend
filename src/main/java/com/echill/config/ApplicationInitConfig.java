package com.echill.config;

import com.echill.entity.Role;
import com.echill.entity.User;
import com.echill.repository.RoleRepository;
import com.echill.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {
    PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository) {
        return args -> {
            // 1. Kiểm tra và tạo Role ADMIN trước (Nếu chưa có thì tạo)
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseGet(() -> {
                        Role newRole = Role.builder()
                                .name("ADMIN")
                                .description("Quản trị viên tối cao của hệ thống")
                                .build();
                        return roleRepository.save(newRole);
                    });

            // 2. Kiểm tra và tạo tài khoản admin
            if (userRepository.findByUsername("admin").isEmpty()) {

                User adminUser = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin"))
                        .fullName("System Administrator")
                        .jobTitle("Super Admin")
                        .build();

                adminUser.addRole(adminRole);

                userRepository.save(adminUser);

                log.warn("Đã khởi tạo thành công tài khoản ADMIN mặc định!");
            }
        };
    }
}
