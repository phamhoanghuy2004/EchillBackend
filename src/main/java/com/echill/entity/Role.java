package com.echill.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;

import java.util.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Role extends BaseEntity {
    @Id
    @Tsid
    Long id;

    @NaturalId
    @Column(nullable = false, unique = true, length = 50)
    String name;

    String description;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    Set<RolePermission> rolePermissions = new HashSet<>();

    public void addPermission(Permission permission) {
        if (permission  != null){
            RolePermission rolePermission = RolePermission.builder()
                    .permission(permission)
                    .role(this)
                    .build();
            this.rolePermissions.add(rolePermission);
        }
    }

    public void removePermission(Permission permission){
        if (permission != null){
            this.rolePermissions.removeIf(rp -> rp.getPermission().equals(permission));
        }
    }

    public void addPermissions(Collection<Permission> permissions) {
        if (permissions != null){
            for (Permission permission : permissions) {
                this.addPermission(permission);
            }
        }
    }

    public void removePermissions(Collection<Permission> permissions){
        if (permissions != null){
            for (Permission p : permissions) {
                this.removePermission(p);
            }
        }
    }

    public void syncPermissions(List<Permission> newPermissions) {
        if (newPermissions == null) return;

        // 1. Lọc và XÓA những quyền CŨ không còn nằm trong danh sách MỚI
        this.rolePermissions.removeIf(rp -> !newPermissions.contains(rp.getPermission()));

        // 2. Lấy ra danh sách các quyền hiện tại đang có
        Set<Permission> currentPermissions = this.rolePermissions.stream()
                .map(RolePermission::getPermission)
                .collect(java.util.stream.Collectors.toSet());

        // 3. Chỉ THÊM những quyền MỚI chưa từng tồn tại trong danh sách cũ
        for (Permission p : newPermissions) {
            if (!currentPermissions.contains(p)) {
                this.addPermission(p);
            }
        }
    }

    public void clearPermissions() {
        this.rolePermissions.clear(); // Nhờ orphanRemoval, Hibernate sẽ tự bắn lệnh DELETE sạch sẽ!
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return Objects.equals(name, role.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
