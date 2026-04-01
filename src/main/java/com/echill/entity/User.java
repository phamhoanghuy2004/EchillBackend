package com.echill.entity;

import com.echill.entity.enums.AuthProvider;
import com.echill.entity.enums.Status;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @NaturalId
    @Column(nullable = false, unique = true , length = 50)
    String username;

    @Column(nullable = false, length = 255)
    String password;

    @Column(nullable = false, unique = true, length = 255)
    String email;

    @Column(name = "full_name", nullable = false, length = 100)
    String fullName;

    String address;

    LocalDate dob;

    @Column(name = "job_title", nullable = false, length = 100)
    String jobTitle;

    @Column(name = "avatar_url")
    String avatarUrl;

    @Column(name = "avatar_public_id")
    String avatarPublicId;

    @Column(name = "current_coin", nullable = false)
    @Builder.Default
    Long currentCoin = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "auth_provider", length = 20)
    @Builder.Default
    AuthProvider authProvider = AuthProvider.SYSTEM;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    Set<UserRole> userRoles = new HashSet<>();

    @Version
    private Long version;

    public void addRole(Role role) {
        if (role != null) {
            UserRole userRole = UserRole.builder()
                    .user(this) // Đóng dấu chủ quyền: Gán Cha cho Con
                    .role(role)
                    .build();
            this.userRoles.add(userRole);
        }
    }

    public void removeRole(Role role) {
        if (role != null) {
            this.userRoles.removeIf(ur -> ur.getRole().equals(role));
        }
    }

    public void clearRoles() {
        this.userRoles.clear(); // Xóa sạch mọi Role của User này (Hibernate tự bắn lệnh DELETE)
    }

    public void deductCoin(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Số xu trừ phải lớn hơn 0");
        }
        if (this.currentCoin < amount) {
            throw new IllegalStateException("Số dư xu không đủ để thực hiện giao dịch!");
        }
        this.currentCoin -= amount;
    }

    public void addCoin(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Số xu nạp phải lớn hơn 0");
        }
        this.currentCoin += amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    // ==========================================
    // DOMAIN LOGIC (Rich Entity - Chuẩn Senior)
    // ==========================================

    public void ensureSystemLogin() {
        if (!AuthProvider.SYSTEM.equals(this.authProvider)) {
            throw new AppException(ErrorEnum.MUST_LOGIN_WITH_GOOGLE);
        }
    }

    public void ensureCanLogin() {
        if (Status.INACTIVE.equals(this.status)) {
            throw new AppException(ErrorEnum.USER_INACTIVE_OR_BLOCKED, Map.of("email", this.email));
        } else if (!Status.ACTIVE.equals(this.status)) {
            throw new AppException(ErrorEnum.USER_INACTIVE_OR_BLOCKED);
        }
    }

    public void ensureActive() {
        if (!Status.ACTIVE.equals(this.status)) {
            throw new AppException(ErrorEnum.USER_INACTIVE_OR_BLOCKED);
        }
    }

    public void ensureInactive() {
        if (!Status.INACTIVE.equals(this.status)) {
            throw new AppException(ErrorEnum.USER_ALREADY_ACTIVE_OR_BLOCKED);
        }
    }

    public void activate() {
        ensureInactive();
        this.status = Status.ACTIVE;
    }

}