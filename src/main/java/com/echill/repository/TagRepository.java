package com.echill.repository;

import com.echill.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    List<Tag> findAllByNameIn(Set<String> names);
    List<Tag> findByParentId(Long parentId);

    @Query("SELECT t.id FROM Tag t WHERE t.parent IS NULL ORDER BY t.id ASC")
    List<Long> findCoreParentTagIds();

}
