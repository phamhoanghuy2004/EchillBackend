package com.echill.repository;

import com.echill.entity.Blog;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    @Query("SELECT b FROM Blog b JOIN FETCH b.user u WHERE u.id = :userId ORDER BY b.createdAt DESC")
    List<Blog> findAllWithUserByUserId(@Param("userId") Long userId);

    @Query("SELECT b FROM Blog b JOIN FETCH b.user WHERE b.id = :id")
    Optional<Blog> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT c.imagePublicId FROM Course c WHERE c.imagePublicId IS NOT NULL")
    List<String> findAllImagePublicIds();

}
