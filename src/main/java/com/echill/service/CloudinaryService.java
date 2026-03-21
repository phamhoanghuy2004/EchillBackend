package com.echill.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CloudinaryService {
    Cloudinary cloudinary;
    long MAX_FILE_SIZE = 2 * 1024 * 1024;

    public String uploadImage(MultipartFile file, String folderName) {
        try {
            // 1. Kiểm tra định dạng file (Phải là ảnh)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("Cảnh báo: Phát hiện file không hợp lệ được gửi lên: {}", contentType);
                throw new AppException(ErrorEnum.INVALID_AVATAR_FORMAT);
            }

            // 2. Kiểm tra dung lượng file (Không quá 2MB)
            if (file.getSize() > MAX_FILE_SIZE) {
                log.warn("Cảnh báo: File tải lên quá lớn ({} bytes)", file.getSize());
                throw new AppException(ErrorEnum.AVATAR_SIZE_TOO_LARGE);
            }

            // Đẩy file lên Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folderName
            ));

            // Lấy ra URL để lưu vào Database
            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            log.error("Lỗi khi upload ảnh vào thư mục {} lên Cloudinary: ", folderName, e);
            throw new AppException(ErrorEnum.CANNOT_UPLOAD_IMAGE);
        }
    }

    // ========================================================
    // 💥 CÁC HÀM MỚI ĐỂ XÓA ẢNH CŨ TRÊN CLOUDINARY
    // ========================================================

    /**
     * Hàm xóa ảnh trên Cloudinary dựa vào URL
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return; // Nếu user chưa có ảnh (URL null) thì bỏ qua, không cần xóa
        }

        try {
            // 1. Tách lấy public_id từ URL
            String publicId = extractPublicId(imageUrl);

            if (publicId != null) {
                // 2. Gọi lệnh xóa của Cloudinary
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Đã dọn dẹp thành công ảnh cũ trên Cloudinary: {}", publicId);
            } else {
                log.warn("Không thể trích xuất public_id từ URL: {}", imageUrl);
            }
        } catch (Exception e) {
            // 💥 Bọc thép: Chỉ log ra lỗi chứ KHÔNG throw Exception
            // Để lỡ Cloudinary lỗi thì user vẫn cập nhật được Profile bình thường trong DB
            log.error("Lỗi khi xóa ảnh trên Cloudinary với URL {}: ", imageUrl, e);
        }
    }

    /**
     * Hàm phụ trợ: Cắt gọt URL để lấy public_id
     * URL mẫu: https://res.cloudinary.com/demo/image/upload/v1612345/echill_avatars/abc123.png
     * Output cần lấy: echill_avatars/abc123
     */
    private String extractPublicId(String imageUrl) {
        try {
            // Tìm vị trí chữ "/upload/"
            int uploadIndex = imageUrl.indexOf("/upload/");
            if (uploadIndex == -1) return null;

            // Cắt phần đầu, chỉ lấy từ sau "/upload/" trở đi: "v1612345/echill_avatars/abc123.png"
            String afterUpload = imageUrl.substring(uploadIndex + 8);

            // Bỏ đi cái version "v1612345/" (nếu có)
            if (afterUpload.matches("^v\\d+/.*")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
            }

            // Bỏ đi đuôi ".png", ".jpg"
            int lastDotIndex = afterUpload.lastIndexOf(".");
            if (lastDotIndex != -1) {
                afterUpload = afterUpload.substring(0, lastDotIndex);
            }

            return afterUpload; // Trả về đúng public_id
        } catch (Exception e) {
            return null;
        }
    }
}