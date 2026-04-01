package com.echill.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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

    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final long MAX_DOC_SIZE = 10 * 1024 * 1024;  // 10MB

    public Map<String, String> uploadImage(MultipartFile file, String folderName) {
        try {
            // 1. Kiểm tra định dạng file (Phải là ảnh)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("Cảnh báo: Phát hiện file không hợp lệ được gửi lên: {}", contentType);
                throw new AppException(ErrorEnum.INVALID_IMAGE_FORMAT);
            }

            // 2. Kiểm tra dung lượng file (Không quá 2MB)
            if (file.getSize() > MAX_IMAGE_SIZE) {
                log.warn("Cảnh báo: File tải lên quá lớn ({} bytes)", file.getSize());
                throw new AppException(ErrorEnum.IMAGE_SIZE_TOO_LARGE);
            }

            // Đẩy file lên Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folderName
            ));

            return Map.of(
                    "url", uploadResult.get("secure_url").toString(),
                    "publicId", uploadResult.get("public_id").toString()
            );

        } catch (IOException e) {
            log.error("Lỗi khi upload ảnh vào thư mục {} lên Cloudinary: ", folderName, e);
            throw new AppException(ErrorEnum.CANNOT_UPLOAD_IMAGE);
        }
    }

    public Map<String, String> uploadDocument(MultipartFile file, String folderName) {
        try {
            // 1. Kiểm tra định dạng file (PDF, Word)
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("application/pdf") &&
                    !contentType.equals("application/msword") &&
                    !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
                log.warn("Cảnh báo: Phát hiện định dạng tài liệu không hợp lệ: {}", contentType);
                throw new AppException(ErrorEnum.INVALID_FILE_FORMAT);
            }

            // 2. Kiểm tra dung lượng file
            if (file.getSize() > MAX_DOC_SIZE) {
                log.warn("Cảnh báo: Tài liệu tải lên quá lớn ({} bytes)", file.getSize());
                throw new AppException(ErrorEnum.FILE_SIZE_TOO_LARGE);
            }

            // 💥 3. LẤY ĐUÔI FILE GỐC ĐỂ ÉP CLOUDINARY NHẬN DIỆN
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                // Lấy phần đuôi (VD: .pdf, .docx)
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Tạo một tên file mới độc nhất, BẮT BUỘC PHẢI CÓ ĐUÔI
            // VD: doc_1710000000000.pdf
            String customPublicId = "doc_" + System.currentTimeMillis() + extension;

            // 4. Đẩy file lên Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folderName,
                    "resource_type", "raw",
                    "public_id", customPublicId // 💥 ÉP CLOUDINARY LƯU TÊN CÓ ĐUÔI!
                    // Xóa use_filename và unique_filename đi vì mình đã tự xử lý ở trên rồi
            ));

            return Map.of(
                    "url", uploadResult.get("secure_url").toString(),
                    "publicId", uploadResult.get("public_id").toString()
            );

        } catch (IOException e) {
            log.error("Lỗi khi upload tài liệu vào thư mục {} lên Cloudinary: ", folderName, e);
            throw new AppException(ErrorEnum.CANNOT_UPLOAD_FILE);
        }
    }
}