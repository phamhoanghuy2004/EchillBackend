package com.echill.service;

import com.echill.constant.CloudinaryFolder;
import com.echill.dto.request.CompleteProfileRequest;
import com.echill.dto.request.UserUpdateRequest;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.UserRepository;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    UserRepository userRepository;
    CloudinaryService cloudinaryService;

    @Transactional
    public void completeProfile(CompleteProfileRequest request) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new AppException(ErrorEnum.USER_NOTFOUND)
        );

        user.setAddress(request.getAddress());
        user.setDob(request.getDob());
        user.setJobTitle(request.getJobTitle());

        userRepository.save(user);
    }


    public void update(UserUpdateRequest request, MultipartFile avatar) {

        Long userid = SecurityUtils.getCurrentUserId();

        // 1. Tìm User trong DB trước (Rất nhanh, tốn 1ms)
        User user = userRepository.findById(userid).orElseThrow(
                () -> new AppException(ErrorEnum.USER_NOTFOUND)
        );

        // 2. Dùng MapStruct để update các trường text cho gọn (nhớ tạo hàm này trong UserMapper)
        // userMapper.updateUser(user, request);
        // (Nếu chưa có Mapper thì tạm xài tay như cũ cũng được)
        user.setFullName(request.getFullName());
        user.setAddress(request.getAddress());
        user.setDob(request.getDob());
        user.setJobTitle(request.getJobTitle());

        // 3. Xử lý Avatar: UPLOAD ẢNH MỚI & XÓA ẢNH CŨ
        if (avatar != null && !avatar.isEmpty()) {
            String oldAvatarUrl = user.getAvatarUrl(); // Giữ lại link ảnh cũ

            // Đẩy ảnh mới lên Cloudinary (Tốn 1-3s, nhưng lúc này DB không bị khóa)
            String newAvatarUrl = cloudinaryService.uploadImage(avatar, CloudinaryFolder.AVATAR);
            user.setAvatarUrl(newAvatarUrl);

            // 💥 GỌI HÀM XÓA ẢNH CŨ (Nếu bạn có viết hàm extract publicId và delete trong CloudinaryService)
            if (oldAvatarUrl != null) {
                try {
                    cloudinaryService.deleteImage(oldAvatarUrl);
                } catch (Exception e) {
                    // Log lỗi ra thôi, không throw Exception để tránh làm hỏng quá trình update
                    log.error("Không thể xóa ảnh avatar cũ trên Cloudinary: {}", oldAvatarUrl, e);
                }
            }
        }

        // 4. Lưu vào DB (Hàm .save() của JpaRepository bản thân nó đã bọc @Transactional sẵn rồi, rất an toàn)
        userRepository.save(user);
    }
}
