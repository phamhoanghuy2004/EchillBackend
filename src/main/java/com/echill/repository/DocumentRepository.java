package com.echill.repository;

import com.echill.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query("SELECT d.documentPublicId FROM Document d WHERE d.documentPublicId IS NOT NULL")
    List<String> findAllDocumentPublicIds();

    // 💥 Spring Data JPA tự hiểu hàm này và mò theo khóa ngoại lesson_id
    List<Document> findByLessonId(Long lessonId);
}
