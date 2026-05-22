package com.echill.repository;

import com.echill.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface UserRepository extends JpaRepository<User,Long>, JpaSpecificationExecutor<User> {
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

    @Query("SELECT u.avatarPublicId FROM User u WHERE u.avatarPublicId IS NOT NULL")
    List<String> findAllAvatarPublicIds();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForPaymentLock(@Param("id") Long id);

    @Query("SELECT COUNT(u) FROM User u JOIN u.userRoles ur JOIN ur.role r WHERE r.name = 'STUDENT'")
    long countStudents();

    @Query("SELECT COUNT(u) FROM User u JOIN u.userRoles ur JOIN ur.role r WHERE r.name = 'TEACHER'")
    long countTeachers();

    @Query("SELECT u.id, u.fullName FROM User u JOIN u.userRoles ur JOIN ur.role r WHERE r.name = 'TEACHER'")
    List<Object[]> findAllTeachersBasicInfo();
}
