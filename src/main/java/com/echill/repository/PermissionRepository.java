package com.echill.repository;

import com.echill.entity.Permission;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    boolean existsByName(String name);

    @Query("SELECT rp.permission FROM RolePermission rp WHERE rp.role.id = :roleId")
    List<Permission> findAllByRoleId(@Param("roleId") Long roleId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Permission p WHERE p.id = :id")
    void deletePermissionById(@Param("id") Long id);
}
