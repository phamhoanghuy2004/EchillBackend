package com.echill.service;

import com.echill.constant.CloudinaryFolder;
import com.echill.dto.request.CompleteProfileRequest;
import com.echill.dto.request.UserUpdateRequest;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.service.persistence.UserPersistenceService;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {

    UserPersistenceService userPersistenceService;
    CloudinaryService cloudinaryService;

    // KHÔNG CÓ @Transactional
    public void completeProfile(CompleteProfileRequest request) {
        // 1. Lấy thẳng ID từ Security Context (Vứt cách lấy Username đi)
        Long userId = SecurityUtils.getCurrentUserId();

        // 2. Gọi DB cập nhật
        userPersistenceService.completeUserProfile(userId, request);
    }

    // KHÔNG CÓ @Transactional
    // KHÔNG CÓ @Transactional
    public void update(UserUpdateRequest request, MultipartFile avatar) {

        Long userId = SecurityUtils.getCurrentUserId();

        // 1. Lấy User để biết link avatar cũ
        User currentUser = userPersistenceService.getUserById(userId);
        String oldAvatarUrl = currentUser.getAvatarUrl();
        String newAvatarUrl = null;

        // 2. UPLOAD ẢNH MỚI LÊN CLOUDINARY
        if (avatar != null && !avatar.isEmpty()) {
            newAvatarUrl = cloudinaryService.uploadImage(avatar, CloudinaryFolder.AVATAR);

            // 💥 FIX LỖI SỐ 1 CỦA BẠN: Nếu upload xịt, văng lỗi luôn, dừng toàn bộ quy trình!
            if (newAvatarUrl == null || newAvatarUrl.isEmpty()) {
                throw new AppException(ErrorEnum.UPLOAD_AVT_FAILED);
            }
        }

        // 3. 💥 CẬP NHẬT DATABASE TRƯỚC (Rất quan trọng)
        // Nếu DB lỗi, nó sẽ văng Exception ở đây và code dừng lại, ảnh cũ chưa bị xóa!
        userPersistenceService.updateUserInfo(userId, request, newAvatarUrl);

        // 4. 💥 XÓA ẢNH CŨ SAU CÙNG
        // Chỉ tiến hành xóa khi: CÓ UP ẢNH MỚI + DB ĐÃ LƯU THÀNH CÔNG + CÓ ẢNH CŨ ĐỂ XÓA
        if (newAvatarUrl != null && oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
            try {
                cloudinaryService.deleteImage(oldAvatarUrl);
                log.info("Đã dọn dẹp xong avatar cũ: {}", oldAvatarUrl);
            } catch (Exception e) {
                // Xóa lỗi thì kệ nó, rác Cloudinary một tí không sao, trải nghiệm User vẫn mượt!
                log.error("Không thể xóa ảnh avatar cũ trên Cloudinary: {}", oldAvatarUrl, e);
            }
        }
    }
}