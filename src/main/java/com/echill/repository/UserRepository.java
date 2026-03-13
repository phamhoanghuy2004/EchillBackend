package com.echill.repository;

import com.echill.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    public Boolean existsByUsername(String username);
    public Boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.userRoles ur " +
            "LEFT JOIN FETCH ur.role r " +
            "LEFT JOIN FETCH r.rolePermissions rp " +
            "LEFT JOIN FETCH rp.permission p " +
            "WHERE u.username = :username")
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("username") String username);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.userRoles ur " +
            "LEFT JOIN FETCH ur.role r " +
            "LEFT JOIN FETCH r.rolePermissions rp " +
            "LEFT JOIN FETCH rp.permission p " +
            "WHERE u.id = :userId")
    Optional<User> findByIdWithRolesAndPermissions(@Param("userId") Long userId);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.userRoles ur " +
            "LEFT JOIN FETCH ur.role r " +
            "LEFT JOIN FETCH r.rolePermissions rp " +
            "LEFT JOIN FETCH rp.permission p " +
            "WHERE u.email = :email")
    Optional<User> findByEmailWithRolesAndPermissions(@Param("email") String email);

    boolean existsByEmail(String email);

}
