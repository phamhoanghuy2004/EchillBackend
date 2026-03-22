package com.echill.repository;

import com.echill.entity.Blog;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    List<Blog> findAllByOrderByCreatedAtDesc();
    @Query("SELECT b FROM Blog b JOIN FETCH b.user u WHERE u.username = :username ORDER BY b.createdAt DESC")
    List<Blog> findByUsername(@Param("username") String username);
}
