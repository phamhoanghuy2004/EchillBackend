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

@Component
@RequiredArgsConstructor
@Slf4j
public class TempSyncRunner implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final CourseDocumentRepository courseDocumentRepository;
    private final CourseDocumentMapper courseDocumentMapper;

    @Override
    @Transactional(readOnly = true) // Bắt buộc có cái này để Mapper không bị dính LazyInitializationException khi gọi Category/Teacher
    public void run(String... args) throws Exception {
        log.info("🚀 [TEMP-RUNNER] BẮT ĐẦU ĐỒNG BỘ DỮ LIỆU TỪ MYSQL SANG ELASTICSEARCH...");

        // 1. Quét sạch sành sanh Data dưới MySQL lên
        List<Course> allCourses = courseRepository.findAll();

        // 2. Lọc hàng xịn (ACTIVE) và chế biến sang ES Document
        List<CourseDocument> documents = allCourses.stream()
                .filter(c -> c.getStatus() == Status.ACTIVE)
                .map(c -> {
                    CourseDocument doc = courseDocumentMapper.toDocument(c);
                    doc.setDiscountPercent(c.getDiscountPercent()); // Ép thêm cái % giảm giá
                    return doc;
                }).toList();

        // 3. Xóa sạch kho cũ (nếu có) để làm ván mới
        courseDocumentRepository.deleteAll();

        // 4. Bắn một phát súng đưa tất cả lên mây
        if (!documents.isEmpty()) {
            courseDocumentRepository.saveAll(documents);
            log.info("✅ [TEMP-RUNNER] ĐÃ ĐỒNG BỘ THÀNH CÔNG {} KHÓA HỌC SANG ES!", documents.size());
        } else {
            log.info("⚠️ [TEMP-RUNNER] KHÔNG CÓ KHÓA HỌC ACTIVE NÀO DƯỚI DB ĐỂ ĐỒNG BỘ!");
        }

        log.info("🗑️ [TEMP-RUNNER] Xong việc rồi, Chủ tịch có thể xóa class TempSyncRunner này đi nhé!");
    }
}