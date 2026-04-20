package com.echill.config; // Vứt tạm vào package nào cũng được

import com.echill.document.CourseDocument;
import com.echill.entity.Course;
import com.echill.entity.enums.Status;
import com.echill.mapper.document.CourseDocumentMapper;
import com.echill.repository.CourseRepository;
import com.echill.repository.elasticsearch.CourseDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TempSyncRunner implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final CourseDocumentRepository courseDocumentRepository;
    private final CourseDocumentMapper courseDocumentMapper;

    @Override
    @Transactional(readOnly = true)
    public void run(String... args) throws Exception {
        log.info("🚀 [TEMP-RUNNER] BẮT ĐẦU ĐỒNG BỘ DỮ LIỆU TỪ MYSQL SANG ELASTICSEARCH...");

        List<Course> allCourses = courseRepository.findAll();
        if (allCourses.isEmpty()) {
            log.info("⚠️ [TEMP-RUNNER] KHÔNG CÓ KHÓA HỌC ACTIVE NÀO!");
            return;
        }

        List<Object[]> rawPairs = courseRepository.findAllCourseTagPairs();
        Map<Long, List<Long>> courseTagsMap = rawPairs.stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (Long) row[1], Collectors.toList())
                ));

        List<CourseDocument> documents = allCourses.stream()
                .map(c -> {
                    CourseDocument doc = courseDocumentMapper.toDocument(c);
                    doc.setDiscountPercent(c.getDiscountPercent());
                    List<Long> tagIds = courseTagsMap.getOrDefault(c.getId(), List.of());
                    doc.setTagIds(tagIds);
                    return doc;
                }).toList();

        courseDocumentRepository.deleteAll();

        if (!documents.isEmpty()) {
            courseDocumentRepository.saveAll(documents);
            log.info("✅ [TEMP-RUNNER] ĐÃ ĐỒNG BỘ THÀNH CÔNG {} KHÓA HỌC SANG ES!", documents.size());
        }

        log.info("🗑️ [TEMP-RUNNER] Xong việc rồi, Chủ tịch có thể xóa class TempSyncRunner này đi nhé!");
    }
}