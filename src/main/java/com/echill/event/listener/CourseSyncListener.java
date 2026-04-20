package com.echill.event.listener;

import com.echill.document.CourseDocument;
import com.echill.entity.Course;
import com.echill.event.CourseCreatedEvent;
import com.echill.event.CourseUpdatedEvent;
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

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseSyncListener {
    CourseDocumentRepository courseDocumentRepository;
    CourseDocumentMapper courseDocumentMapper;
    CourseRepository courseRepository;

    @Async("ioTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, classes = {CourseCreatedEvent.class, CourseUpdatedEvent.class})
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void syncCourseToElasticsearch(Object event) {
        try {
            Long courseId = extractCourseId(event);
            log.info("[ES-Sync] Luồng {} đang đồng bộ Course ID: {}", Thread.currentThread().getName(), courseId);

            Course savedCourse = courseRepository.findByIdWithDetails(courseId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Course ID: " + courseId));

            List<Long> tagIds = courseRepository.findTagIdsByCourseId(courseId);

            CourseDocument doc = courseDocumentMapper.toDocument(savedCourse);
            doc.setDiscountPercent(savedCourse.getDiscountPercent());
            doc.setTagIds(tagIds);

            courseDocumentRepository.save(doc);

            log.info("[ES-Sync] Đồng bộ thành công!");
        } catch (Exception e) {
            log.error("[ES-Sync] Lỗi đồng bộ Course sang Elasticsearch!", e);
        }
    }

    private Long extractCourseId(Object event) {
        if (event instanceof CourseCreatedEvent(Long courseId)) {
            return courseId;
        } else if (event instanceof CourseUpdatedEvent(Long courseId)) {
            return courseId;
        }
        throw new IllegalArgumentException("Hệ thống nhận được Event không hợp lệ: " + event.getClass().getName());
    }
}
