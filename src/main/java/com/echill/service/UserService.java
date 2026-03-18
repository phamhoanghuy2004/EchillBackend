package com.echill.service;

import com.echill.dto.request.CompleteProfileRequest;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;

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
}
