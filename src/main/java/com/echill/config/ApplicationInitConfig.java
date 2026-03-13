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
import org.springframework.transaction.support.TransactionTemplate; // Thêm import này

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    static final Map<String, String> DEFAULT_ROLES = Map.of(
            "ADMIN", "Quản trị viên tối cao của hệ thống",
            "STUDENT", "Học viên của nền tảng Echill",
            "TEACHER", "Giảng viên giảng dạy và tạo khóa học"
    );

    PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner applicationRunner(
            UserRepository userRepository,
            RoleRepository roleRepository,
            TransactionTemplate transactionTemplate) {

        return args -> {

            transactionTemplate.execute(status -> {

                Map<String, Role> roleCache = new HashMap<>();

                DEFAULT_ROLES.forEach((roleName, description) -> {
                    Role role = roleRepository.findByName(roleName)
                            .orElseGet(() -> roleRepository.save(Role.builder()
                                    .name(roleName)
                                    .description(description)
                                    .build()));

                    roleCache.put(roleName, role);
                });

                log.info("Đã kiểm tra và đồng bộ các Role cốt lõi ({}) thành công!", String.join(", ", DEFAULT_ROLES.keySet()));

                if (userRepository.findByUsername("admin").isEmpty()) {

                    User adminUser = User.builder()
                            .username("admin")
                            .password(passwordEncoder.encode("admin"))
                            .email("admin@gmail.com")
                            .fullName("System Administrator")
                            .jobTitle("Super Admin")
                            .build();

                    Role adminRole = roleCache.get("ADMIN");
                    adminUser.addRole(adminRole);

                    userRepository.save(adminUser);

                    log.warn("⚠️ Đã khởi tạo thành công tài khoản ADMIN mặc định!");
                }

                return null;
            });
        };
    }
}