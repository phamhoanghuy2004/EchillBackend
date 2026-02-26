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
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long id;

    @NaturalId
    @Column(nullable = false, unique = true, length = 50)
    String name;

    String description;

    @Setter(AccessLevel.NONE)
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    Set<Permission> permissions = new HashSet<>();


    public void addPermission(Permission permission) {
        if (permission != null) {
            this.permissions.add(permission);
        }
    }

    public void removePermission(Permission permission) {
        if (permission != null) {
            this.permissions.remove(permission);
        }
    }

    public void addPermissions(Collection<Permission> permissions) {
        if (permissions != null) {
            this.permissions.addAll(permissions);
        }
    }

    public void removePermissions(Collection<Permission> oldPermissions) {
        if (oldPermissions != null && !oldPermissions.isEmpty()) {
            this.permissions.removeAll(oldPermissions);
        }
    }

    public void clearPermissions() {
        this.permissions.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return Objects.equals(name, role.name); // ham nay la de so sanh an toan, khong bi loi null pointer
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
