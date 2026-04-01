package com.echill.service.persistence;

import com.echill.dto.request.CompleteProfileRequest;
import com.echill.dto.request.UserUpdateRequest;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserPersistenceService {

    UserRepository userRepository;

    @Transactional
    public void updateUserInfo(User user, UserUpdateRequest request, String newAvatarUrl, String newAvatarPublicId) {

        // Cập nhật thông tin
        user.setFullName(request.getFullName());
        user.setAddress(request.getAddress());
        user.setDob(request.getDob());
        user.setJobTitle(request.getJobTitle());

        // Nếu có ảnh mới thì set vào, không thì giữ nguyên ảnh cũ
        if (newAvatarUrl != null) {
            user.setAvatarUrl(newAvatarUrl);
            user.setAvatarPublicId(newAvatarPublicId); // Nhớ tạo cột này trong Entity User
        }

        userRepository.save(user);
    }
}