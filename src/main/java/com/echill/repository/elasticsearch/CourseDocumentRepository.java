package com.echill.repository.elasticsearch;

import com.echill.document.CourseDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CourseDocumentRepository extends ElasticsearchRepository<CourseDocument, Long> {
}