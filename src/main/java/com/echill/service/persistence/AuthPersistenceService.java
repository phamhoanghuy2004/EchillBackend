package com.echill.service.persistence;

import com.echill.dto.request.UserRegisterRequest;
import com.echill.entity.*;
import com.echill.entity.enums.AuthProvider;
import com.echill.entity.enums.Status;
import com.echill.event.UserAuthEvent;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.mapper.UserMapper;
import com.echill.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthPersistenceService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    StudentProfileRepository studentProfileRepository;
    TeacherProfileRepository teacherProfileRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;
    UserMapper userMapper;
    ApplicationEventPublisher eventPublisher;

    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_TEACHER = "TEACHER";

    @Transactional
    public void saveNewUser(UserRegisterRequest request, String encodedPassword, String avatarUrl, String avatarPublicId) {
        User user = userMapper.toUser(request);
        user.setPassword(encodedPassword);
        user.setStatus(Status.INACTIVE);
        user.setAvatarUrl(avatarUrl);
        user.setAvatarPublicId(avatarPublicId);

        Role userRole = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new AppException(ErrorEnum.ROLE_NOT_EXIST));

        user.addRole(userRole);
        user = userRepository.save(user);

        createProfileForUser(user, request.getRole());
        // 💥 BẮN EVENT (Nhưng Listener sẽ tự động chờ đến khi DB Commit mới chạy)
        eventPublisher.publishEvent(new UserAuthEvent(user.getEmail(), user.getFullName(), false));
    }

    @Transactional
    public User activateUser(String email) {
        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        user.activate(); // Dùng logic DDD ở Entity
        return userRepository.save(user);
    }

    @Transactional
    public void updatePassword(String email, String encodedNewPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));
        user.ensureSystemLogin(); // DDD
        user.ensureActive(); // DDD
        user.setPassword(encodedNewPassword);
        userRepository.save(user);
    }

    @Transactional
    public User processGoogleUserInTx(String email, String name, String pictureUrl, String roleStr, String randomPassword) {
        var userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) return userOptional.get();

        Role userRole = roleRepository.findByName(roleStr)
                .orElseThrow(() -> new AppException(ErrorEnum.ROLE_NOT_EXIST));

        String randomSuffix = UUID.randomUUID().toString().substring(0, 5);
        String generatedUsername = "google_" + email.split("@")[0] + "_" + randomSuffix;

        User newUser = User.builder()
                .email(email).fullName(name).username(generatedUsername)
                .password(randomPassword).avatarUrl(pictureUrl)
                .status(Status.ACTIVE).jobTitle("").authProvider(AuthProvider.GG)
                .build();

        newUser.addRole(userRole);
        userRepository.save(newUser);

        createProfileForUser(newUser, roleStr);
        return newUser;
    }

    @Transactional
    public void saveInvalidatedToken(String jti, Date expiryTime) {
        invalidatedTokenRepository.save(InvalidatedToken.builder().id(jti).expiryTime(expiryTime).build());
    }

    @Transactional
    public User blacklistTokenAndGetUser(String jti, Date expiryTime, String username) {
        saveInvalidatedToken(jti, expiryTime);
        return userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new AppException(ErrorEnum.UNAUTHENTICATED));
    }

    private void createProfileForUser(User user, String roleStr) {
        if (ROLE_STUDENT.equals(roleStr)) {
            studentProfileRepository.save(StudentProfile.builder().user(user).build());
        } else if (ROLE_TEACHER.equals(roleStr)) {
            teacherProfileRepository.save(TeacherProfile.builder().user(user).build());
        }
    }
}