package com.echill.job;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.echill.constant.CloudinaryFolder;
import com.echill.repository.BlogRepository;
import com.echill.repository.CourseRepository;
import com.echill.repository.UserRepository;
import com.echill.repository.DocumentRepository;
import com.echill.repository.LessonRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CloudinaryCleanupJob {

    CourseRepository courseRepository;
    UserRepository userRepository;
    BlogRepository blogRepository;
    DocumentRepository documentRepository;
    LessonRepository lessonRepository;
    Cloudinary cloudinary;

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOrphanCourseImages() {
        log.info("🧹 [COURSE] Khởi động tiến trình dọn rác ảnh Khóa Học...");
        List<String> validIds = courseRepository.findAllImagePublicIds();
        processCleanup(CloudinaryFolder.COURSE_IMAGE, validIds, "Course Images", "image"); // Thêm "image"
    }

    @Scheduled(cron = "0 5 2 * * ?")
    public void cleanupOrphanAvatars() {
        log.info("🧹 [AVATAR] Khởi động tiến trình dọn rác ảnh Avatar...");
        List<String> validIds = userRepository.findAllAvatarPublicIds();
        processCleanup(CloudinaryFolder.AVATAR, validIds, "Avatars", "image");
    }

    @Scheduled(cron = "0 10 2 * * ?")
    public void cleanupOrphanBlogImage() {
        log.info("🧹 [BLOG] Khởi động tiến trình dọn rác ảnh Blog...");
        List<String> validIds = blogRepository.findAllImagePublicIds();
        processCleanup(CloudinaryFolder.BLOG_IMAGE, validIds, "Blog Images", "image");
    }

    // 💥 1. THÊM JOB QUÉT DOCUMENT (Chạy 02:15 AM) - Type là "raw"
    @Scheduled(cron = "0 15 2 * * ?")
    public void cleanupOrphanDocuments() {
        log.info("🧹 [DOCUMENT] Khởi động tiến trình dọn rác Tài liệu (PDF/Word)...");
        List<String> validIds = documentRepository.findAllDocumentPublicIds();
        processCleanup(CloudinaryFolder.DOCUMENT, validIds, "Documents", "raw"); // 💥 Type: raw
    }

    // 💥 2. THÊM JOB QUÉT VIDEO (Chạy 02:20 AM) - Type là "video"
    @Scheduled(cron = "0 20 2 * * ?")
    public void cleanupOrphanVideos() {
        log.info("🧹 [VIDEO] Khởi động tiến trình dọn rác Video Bài giảng...");
        // Giả sử cột lưu ID video trên mây trong bảng Lesson tên là publicVideoId
        List<String> validIds = lessonRepository.findAllVideoPublicIds();
        // Thay CloudinaryFolder.VIDEO bằng folder hằng số tương ứng của bạn
        processCleanup(CloudinaryFolder.LESSION_VIDEO, validIds, "Lesson Videos", "video"); // 💥 Type: video
    }


    /**
     * ⚙️ HÀM LÕI ĐÃ ĐƯỢC NÂNG CẤP (Hỗ trợ Ảnh, Video, File Raw)
     */
    private void processCleanup(String folderName, List<String> validIdsList, String logEntityName, String resourceType) {
        try {
            Set<String> validPublicIds = validIdsList.stream()
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .collect(Collectors.toSet());

            List<String> orphanPublicIds = new ArrayList<>();
            String nextCursor = null;

            do {
                Map<String, Object> params = new HashMap<>();
                params.put("type", "upload");
                params.put("prefix", folderName + "/");
                params.put("max_results", 500);
                params.put("resource_type", resourceType); // 💥 BẮT BUỘC PHẢI CÓ DÒNG NÀY ĐỂ TÌM ĐÚNG LOẠI FILE

                if (nextCursor != null) {
                    params.put("next_cursor", nextCursor);
                }

                Map<?, ?> result = cloudinary.api().resources(params);
                List<Map<?, ?>> resources = (List<Map<?, ?>>) result.get("resources");

                Instant oneHourAgo = Instant.now().minusSeconds(3600);

                for (Map<?, ?> resource : resources) {
                    String cloudPublicId = (String) resource.get("public_id");
                    String createdAtStr = (String) resource.get("created_at");

                    Instant createdAt = Instant.parse(createdAtStr);

                    if (!validPublicIds.contains(cloudPublicId) && createdAt.isBefore(oneHourAgo)) {
                        orphanPublicIds.add(cloudPublicId);
                    }
                }

                nextCursor = (String) result.get("next_cursor");

            } while (nextCursor != null);

            if (!orphanPublicIds.isEmpty()) {
                log.info("🗑️ [{}] Phát hiện {} rác cũ. Đang tiến hành xóa...", logEntityName, orphanPublicIds.size());

                for (int i = 0; i < orphanPublicIds.size(); i += 100) {
                    int end = Math.min(i + 100, orphanPublicIds.size());
                    List<String> batchToDelete = orphanPublicIds.subList(i, end);

                    // 💥 KHI XÓA CŨNG PHẢI BÁO CHO CLOUDINARY BIẾT ĐANG XÓA LOẠI FILE NÀO
                    cloudinary.api().deleteResources(batchToDelete, ObjectUtils.asMap("resource_type", resourceType));
                }

                log.info("✅ [{}] Dọn rác thành công!", logEntityName);
            } else {
                log.info("✨ [{}] Không phát hiện rác, thư mục đang rất sạch sẽ.", logEntityName);
            }

        } catch (Exception e) {
            log.error("❌ [{}] Tiến trình dọn rác gặp sự cố: ", logEntityName, e);
        }
    }
}