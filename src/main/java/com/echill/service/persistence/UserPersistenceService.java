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

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new AppException(ErrorEnum.USER_NOTFOUND)
        );
    }

    @Transactional
    public void completeUserProfile(Long userId, CompleteProfileRequest request) {
        User user = getUserById(userId);

        user.setAddress(request.getAddress());
        user.setDob(request.getDob());
        user.setJobTitle(request.getJobTitle());

        // Dirty checking sẽ tự lưu, hoặc bạn gọi .save() cho chắc cú đều được
        userRepository.save(user);
    }

    @Transactional
    public void updateUserInfo(Long userId, UserUpdateRequest request, String newAvatarUrl) {
        User user = getUserById(userId);

        user.setFullName(request.getFullName());
        user.setAddress(request.getAddress());
        user.setDob(request.getDob());
        user.setJobTitle(request.getJobTitle());

        // Nếu có URL ảnh mới thì cập nhật, không thì giữ nguyên ảnh cũ
        if (newAvatarUrl != null) {
            user.setAvatarUrl(newAvatarUrl);
        }

        userRepository.save(user);
    }
}