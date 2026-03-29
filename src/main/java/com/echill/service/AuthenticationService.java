package com.echill.service;

import com.echill.constant.CloudinaryFolder;
import com.echill.dto.request.*;
import com.echill.dto.response.AuthenticationResponse;
import com.echill.dto.response.IntrospectResponse;
import com.echill.entity.User;
import com.echill.event.UserAuthEvent;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.UserRepository;
import com.echill.service.persistence.AuthPersistenceService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.text.ParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationService {

    AuthPersistenceService authPersistenceService;
    JwtTokenService jwtTokenService;
    OtpService otpService;
    CloudinaryService cloudinaryService;
    GoogleIdTokenVerifier googleVerifier;
    PasswordEncoder passwordEncoder;
    UserRepository userRepository;
    ApplicationEventPublisher eventPublisher;

    public void register(UserRegisterRequest request, MultipartFile avatar) {
        if (userRepository.existsByUsername(request.getUsername())) throw new AppException(ErrorEnum.USERNAME_EXISTED);
        if (userRepository.existsByEmail(request.getEmail())) throw new AppException(ErrorEnum.EMAIL_ALREADY_EXISTS);

        String avatarUrl = (avatar != null && !avatar.isEmpty())
                ? cloudinaryService.uploadImage(avatar, CloudinaryFolder.AVATAR) : null;
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        authPersistenceService.saveNewUser(request, encodedPassword, avatarUrl);
        log.info("Request đăng ký cho {} đã được tiếp nhận.", request.getUsername());
    }

    public AuthenticationResponse verifyRegisterOtp(VerifyOtpRequest request) {
        String email = request.getEmail();
        otpService.validateOtp(email, request.getOtpCode(), false);

        User activatedUser = authPersistenceService.activateUser(email);

        otpService.clearOtp(email, false);
        log.info("✅ User {} đã xác thực OTP thành công.", email);

        return buildAuthResponse(activatedUser, false);
    }

    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        otpService.validateOtp(email, request.getOtpCode(), true);

        authPersistenceService.updatePassword(email, passwordEncoder.encode(request.getNewPassword()));

        otpService.clearOtp(email, true);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        resendOtp(request.getEmail(), true);
    }

    public void resendOtp(String email, boolean isForgot) {
        otpService.validateOtpResendCooldown(email, isForgot);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        if (isForgot) {
            user.ensureSystemLogin();
            user.ensureActive();
        } else {
            user.ensureInactive();
        }

        eventPublisher.publishEvent(new UserAuthEvent(user.getEmail(), user.getFullName(), isForgot));
        log.info("🔄 Đã phát lệnh gửi lại OTP cho user: {}", email);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByUsernameWithRolesAndPermissions(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        user.ensureSystemLogin();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) throw new AppException(ErrorEnum.UNAUTHENTICATED);

        user.ensureCanLogin();

        return buildAuthResponse(user, false);
    }

    public AuthenticationResponse googleLogin(GoogleLoginRequest request) {
        try {
            GoogleIdToken idToken = googleVerifier.verify(request.getCredential());
            if (idToken == null) throw new AppException(ErrorEnum.UNAUTHENTICATED);

            GoogleIdToken.Payload payload = idToken.getPayload();
            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                log.warn("Cảnh báo bảo mật: Email Google chưa được verify!");
                throw new AppException(ErrorEnum.UNAUTHENTICATED);
            }

            String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

            User targetUser = authPersistenceService.processGoogleUserInTx(
                    payload.getEmail(),
                    (String) payload.get("name"),
                    (String) payload.get("picture"),
                    request.getRole(),
                    randomPassword
            );

            boolean isProfileIncomplete = targetUser.getJobTitle() == null || targetUser.getJobTitle().isEmpty()
                    || targetUser.getDob() == null || targetUser.getAddress() == null || targetUser.getAddress().isEmpty();

            return buildAuthResponse(targetUser, isProfileIncomplete);

        } catch (Exception e) {
            log.error("Lỗi xác thực Google Token", e);
            throw new AppException(ErrorEnum.UNAUTHENTICATED);
        }
    }

    public IntrospectResponse introspect(IntrospectRequest introspectRequest) {
        boolean isValid = true;
        try { jwtTokenService.verifyToken(introspectRequest.getToken(), false); }
        catch (AppException e) { isValid = false; }
        return IntrospectResponse.builder().valid(isValid).build();
    }

    public void logout(LogoutRequest logoutRequest) {
        try {
            var signToken = jwtTokenService.verifyToken(logoutRequest.getToken(), true);
            String jti = signToken.getJWTClaimsSet().getJWTID();
            Date refreshExpiryTime = new Date(signToken.getJWTClaimsSet().getIssueTime().toInstant()
                    .plus(jwtTokenService.getREFRESHABLE_DURATION(), ChronoUnit.SECONDS).toEpochMilli());

            authPersistenceService.saveInvalidatedToken(jti, refreshExpiryTime);
            log.info("Đã đưa Token vào danh sách đen: {}", jti);
        } catch (AppException e) {
            log.info("Token đã hết hạn hoặc không hợp lệ. Đăng xuất hoàn thành.");
        } catch (ParseException e) {
            log.warn("Lỗi trích xuất thông tin Token khi đăng xuất");
        }
    }

    public AuthenticationResponse refreshToken(RefreshRequest request) {
        SignedJWT signedJWT = jwtTokenService.verifyToken(request.getToken(), true);
        try {
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            Date refreshExpiryTime = new Date(signedJWT.getJWTClaimsSet().getIssueTime().toInstant()
                    .plus(jwtTokenService.getREFRESHABLE_DURATION(), ChronoUnit.SECONDS).toEpochMilli());
            String username = signedJWT.getJWTClaimsSet().getSubject();

            User user = authPersistenceService.blacklistTokenAndGetUser(jti, refreshExpiryTime, username);

            return buildAuthResponse(user, false);

        } catch (ParseException e) {
            log.error("Lỗi trích xuất thông tin JWT", e);
            throw new AppException(ErrorEnum.UNAUTHENTICATED);
        }
    }

    private AuthenticationResponse buildAuthResponse(User user, boolean isFirstTime) {
        return AuthenticationResponse.builder()
                .authenticated(true)
                .token(jwtTokenService.generateToken(user))
                .isFirstTime(isFirstTime)
                .build();
    }
}