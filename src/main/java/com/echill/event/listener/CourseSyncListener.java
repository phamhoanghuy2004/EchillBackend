package com.echill.event.listener;

import com.echill.document.CourseDocument;
import com.echill.entity.Course;
import com.echill.event.CourseSyncEvent;
import com.echill.mapper.document.CourseDocumentMapper;
import com.echill.repository.CourseRepository;
import com.echill.repository.elasticsearch.CourseDocumentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseSyncListener {
    CourseDocumentRepository courseDocumentRepository;
    CourseDocumentMapper courseDocumentMapper;
    CourseRepository courseRepository;

    @Async("ioTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true) // 💥 Bật Transaction mới cho luồng Async này
    public void syncCourseToElasticsearch(CourseSyncEvent event) { // 💥 Nhận Event chứa ID
        try {
            Long courseId = event.courseId();
            log.info("[ES-Sync] Luồng {} đang đồng bộ Course ID: {}", Thread.currentThread().getName(), courseId);

            // 💥 Tự móc data mới cứng từ DB lên (Kéo luôn Category và Teacher để tránh N+1)
            Course savedCourse = courseRepository.findByIdWithDetails(courseId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Course ID: " + courseId));

            CourseDocument doc = courseDocumentMapper.toDocument(savedCourse);
            doc.setDiscountPercent(savedCourse.getDiscountPercent());
            courseDocumentRepository.save(doc);

            log.info("[ES-Sync] Đồng bộ thành công!");
        } catch (Exception e) {
            log.error("[ES-Sync] Lỗi đồng bộ Course sang Elasticsearch!", e);
        }
    }
}
