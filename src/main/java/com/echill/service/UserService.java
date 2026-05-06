package com.echill.service;

import com.echill.constant.CloudinaryFolder;
import com.echill.dto.request.CompleteProfileRequest;
import com.echill.dto.request.UserUpdateRequest;
import com.echill.dto.response.UserResponse;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.UserRepository;
import com.echill.service.persistence.UserPersistenceService;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {

    UserPersistenceService userPersistenceService;
    CloudinaryService cloudinaryService;
    UserRepository userRepository;
    com.echill.mapper.UserMapper userMapper;

    public UserResponse getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public void completeProfile(CompleteProfileRequest request) {
        // 1. Lấy thẳng ID từ Security Context
        Long userId = SecurityUtils.getCurrentUserId();

        // 2. Kéo User lên (Lúc này User đang là Managed Entity vì nằm trong Transaction)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        // 3. Cập nhật thông tin
        user.setAddress(request.getAddress());
        user.setDob(request.getDob());
        user.setJobTitle(request.getJobTitle());

        // 💥 CHUẨN HIBERNATE: Không cần gọi userRepository.save(user)!
        // Cơ chế Dirty Checking sẽ tự động so sánh và bắn lệnh UPDATE xuống DB khi hàm này kết thúc.
        log.info("Đã cập nhật profile thành công cho User ID: {}", userId);
    }

    public void update(UserUpdateRequest request, MultipartFile avatar) {

        Long userId = SecurityUtils.getCurrentUserId();

        // 1. Kéo User lên ngay tại Service để Validate (Nhanh, không giam DB)
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        // 2. Upload ảnh mới lên Cloudinary (Tốn vài giây, DB vẫn rảnh)
        String newAvatarUrl = null;
        String newAvatarPublicId = null;

        if (avatar != null && !avatar.isEmpty()) {
            Map<String, String> uploadResult = cloudinaryService.uploadImage(avatar, CloudinaryFolder.AVATAR);
            newAvatarUrl = uploadResult.get("url");
            newAvatarPublicId = uploadResult.get("publicId");
        }

        userPersistenceService.updateUserInfo(currentUser, request, newAvatarUrl, newAvatarPublicId);
    }
}