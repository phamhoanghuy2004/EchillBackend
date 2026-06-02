package com.echill.config;

import com.echill.document.CourseDocument;
import com.echill.entity.Course;
import com.echill.mapper.document.CourseDocumentMapper;
import com.echill.repository.CourseRepository;
import com.echill.repository.elasticsearch.CourseDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
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

    // Inject thêm ElasticsearchOperations để thao tác trực tiếp với Index
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    @Transactional(readOnly = true)
    public void run(String... args) throws Exception {
        log.info("🚀 [TEMP-RUNNER] BẮT ĐẦU ĐỒNG BỘ DỮ LIỆU TỪ MYSQL SANG ELASTICSEARCH...");

        // 1. DIỆT CỎ TẬN GỐC TRONG ELASTICSEARCH TRƯỚC TIÊN
        // Cho đoạn này lên đầu để đảm bảo ES luôn được làm sạch mỗi khi chạy Runner
        IndexOperations indexOps = elasticsearchOperations.indexOps(CourseDocument.class);
        if (indexOps.exists()) {
            log.info("🗑️ [TEMP-RUNNER] Tìm thấy Index cũ, tiến hành xóa toàn bộ...");
            indexOps.delete();
        }

        log.info("✨ [TEMP-RUNNER] Khởi tạo lại Index mới với Mapping chuẩn...");
        indexOps.create();
        indexOps.putMapping(indexOps.createMapping());

        // 2. Bắt đầu lấy dữ liệu từ MySQL
        List<Course> allCourses = courseRepository.findAll();
        if (allCourses.isEmpty()) {
            // Giờ thì return thoải mái vì ES đã bị dọn dẹp sạch sẽ ở bước 1 rồi
            log.info("⚠️ [TEMP-RUNNER] KHÔNG CÓ KHÓA HỌC NÀO TRONG MYSQL! (Elasticsearch cũng đã được dọn sạch)");
            return;
        }

        // 3. Map dữ liệu
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

        // 4. Save vào Elasticsearch
        if (!documents.isEmpty()) {
            courseDocumentRepository.saveAll(documents);
            log.info("✅ [TEMP-RUNNER] ĐÃ ĐỒNG BỘ THÀNH CÔNG {} KHÓA HỌC SANG ES!", documents.size());
        }
    }
}