package com.echill.entity;

import com.echill.entity.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.NaturalId;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends BaseEntity {
    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    @NaturalId
    @Column(unique = true , length = 50, nullable = false)
    String username;

    @Column(nullable = false, length = 100)
    String password;

    @Column(name = "full_name", nullable = false, length = 100)
    String fullName;

    String address;

    LocalDate dob;

    @Column(name = "job_title", nullable = false, length = 100)
    String jobTitle;

    @Column(name = "avatar_url")
    String avatarUrl;

    @Column(name = "current_coin", nullable = false)
    @Builder.Default
    Long currentCoin = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    UserStatus status = UserStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    Set<Role> roles = new HashSet<>();


    public void addRole(Role role) {
        if (role != null) {
            this.roles.add(role);
        }
    }

    public void removeRole(Role role) {
        if (role != null) {
            this.roles.remove(role);
        }
    }

    public void addRoles(Collection<Role> roles) {
        if (roles != null) {
            this.roles.addAll(roles);
        }
    }

    public void removeRoles(Collection<Role> roles) {
        if (roles != null && !roles.isEmpty()) {
            this.roles.removeAll(roles);
        }
    }

    public void clearRoles() {
        this.roles.clear();
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

}
