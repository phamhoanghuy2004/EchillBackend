package com.echill.service;

import com.echill.dto.request.*;
import com.echill.dto.response.AuthenticationResponse;
import com.echill.dto.response.IntrospectResponse;
import com.echill.entity.*;
import com.echill.entity.enums.AuthProvider;
import com.echill.entity.enums.Level;
import com.echill.entity.enums.Status;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.mapper.UserMapper;
import com.echill.repository.*;
import com.github.f4b6a3.tsid.TsidCreator;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.echill.dto.request.GoogleLoginRequest;
import java.util.Collections;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.StringJoiner;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationService {
    @NonFinal
    @Value("${jwt.signerKey}")
    String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    @NonFinal
    @Value("${google.client-id}")
    protected String GOOGLE_CLIENT_ID;

    @NonFinal
    JWSVerifier verifier;

    @NonFinal
    JWSSigner signer;

    InvalidatedTokenRepository invalidatedTokenRepository;

    UserRepository userRepository;

    RoleRepository roleRepository;

    PasswordEncoder passwordEncoder;

    UserMapper userMapper;

    StudentProfileRepository studentProfileRepository;

    TeacherProfileRepository teacherProfileRepository;

    RedisTemplate<String, String> redisTemplate;

    EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String OTP_PREFIX = "otp:register:";

    private static final String RESET_OTP_PREFIX = "otp:forgotPassword:";

    private static final int OTP_EXPIRATION_MINUTES = 5;

    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;

    @PostConstruct
    public void init() throws JOSEException {
        byte[] sharedKey = SIGNER_KEY.getBytes(StandardCharsets.UTF_8);
        this.verifier = new MACVerifier(sharedKey);
        this.signer = new MACSigner(sharedKey);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        String redisKey = RESET_OTP_PREFIX + email;

        // 1. FAIL-FAST: Kiểm tra OTP trên Redis TRƯỚC!
        // Nếu sai hoặc hết hạn thì văng lỗi luôn, đỡ tốn công Query DB
        validateOtp(redisKey, request.getOtpCode());

        // 2. Kéo User từ DB lên để xử lý
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        // 3. Kiểm tra các điều kiện an toàn của User
        if (!AuthProvider.SYSTEM.equals(user.getAuthProvider())) {
            throw new AppException(ErrorEnum.MUST_LOGIN_WITH_GOOGLE);
        }

        if (!Status.ACTIVE.equals(user.getStatus())) {
            throw new AppException(ErrorEnum.USER_INACTIVE_OR_BLOCKED);
        }

        // 4. Mã hóa và cập nhật mật khẩu
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);

        // 5. BẢO MẬT TUYỆT ĐỐI: Dọn dẹp OTP để chặn việc tái sử dụng
        redisTemplate.delete(redisKey);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        // Tận dụng luôn hàm resendOtp vì logic kiểm tra và gửi mail của luồng Quên mật khẩu là y hệt nhau!
        resendOtp(request.getEmail(), true);
    }

    @Transactional
    public AuthenticationResponse verifyRegisterOtp(VerifyOtpRequest request) {
        String email = request.getEmail();
        String inputOtp = request.getOtpCode();
        String redisKey = OTP_PREFIX + email;

        // 1. Validate opt trước
        validateOtp(redisKey, inputOtp);

        // 2. Tìm user theo email
        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        // 3. Kiểm tra trạng thái: Nếu đã ACTIVE thì không cần xác thực nữa
        if (!Status.INACTIVE.equals(user.getStatus())) {
            throw new AppException(ErrorEnum.USER_ALREADY_ACTIVE_OR_BLOCKED);
        }

        // 4. Vượt qua hết -> Xác thực thành công: Đổi trạng thái User thành ACTIVE
        user.setStatus(Status.ACTIVE);
        user = userRepository.save(user); // Lưu cập nhật vào DB

        // 5. CLEAN UP: Xoá ngay OTP trên Redis để tránh bị tái sử dụng (Bảo mật)
        redisTemplate.delete(redisKey);

        // 6. AUTO-LOGIN: Sinh Token và trả về cho Frontend đăng nhập luôn
        String token = generateToken(user);

        log.info("✅ User {} đã xác thực OTP thành công và tự động đăng nhập.", email);

        return AuthenticationResponse.builder()
                .authenticated(true)
                .token(token)
                .isFirstTime(false)
                .build();
    }

    public void resendOtp (String email, boolean isForgot) {
        // 1. FAIL-FAST: Dựng khiên Redis lên đỡ đạn TRƯỚC!
        String redisKey = isForgot ? RESET_OTP_PREFIX + email : OTP_PREFIX + email;
        validateOtpResendCooldown(redisKey);

        // 2. Kiểm tra user có tồn tại không
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        // 3. Phân luồng kiểm tra điều kiện hợp lệ
        if (isForgot) {
            // 🛑 Luồng Quên mật khẩu: User phải ACTIVE và không phải tài khoản Google
            if (!AuthProvider.SYSTEM.equals(user.getAuthProvider())) {
                throw new AppException(ErrorEnum.MUST_LOGIN_WITH_GOOGLE);
            }
            if (!Status.ACTIVE.equals(user.getStatus())) {
                throw new AppException(ErrorEnum.USER_INACTIVE_OR_BLOCKED);
            }
        } else {
            // 🛑 Luồng Xác thực đăng ký mới: User phải INACTIVE
            if (!Status.INACTIVE.equals(user.getStatus())) {
                throw new AppException(ErrorEnum.USER_ALREADY_ACTIVE_OR_BLOCKED);
            }
        }

        // 4. Tạo và gửi lại OTP
        generateAndSendOtp(email, user.getFullName(),isForgot);

        log.info("🔄 Đã gửi lại OTP cho user: {}", email);
    }

    @Transactional
    public void register (UserRegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorEnum.USERNAME_EXISTED);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorEnum.EMAIL_ALREADY_EXISTS);
        }

        User newUser = userMapper.toUser(request);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setStatus(Status.INACTIVE);

        Role userRole = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new AppException(ErrorEnum.ROLE_NOT_EXIST));

        newUser.addRole(userRole);
        newUser = userRepository.save(newUser);

        if ("STUDENT".equals(request.getRole())) {
            studentProfileRepository.save(StudentProfile.builder().user(newUser).build());
        } else if ("TEACHER".equals(request.getRole())) {
            teacherProfileRepository.save(TeacherProfile.builder().user(newUser).build());
        }

        generateAndSendOtp(newUser.getEmail(), newUser.getFullName(), false);

        log.info("Đã tạo user {} và phát hành OTP", request.getUsername());

    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // 1. Kéo user từ DB lên
        var user = userRepository.findByUsernameWithRolesAndPermissions(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        // 2. Chặn đăng nhập tay nếu là tài khoản Google
        if (!AuthProvider.SYSTEM.equals(user.getAuthProvider())) {
            throw new AppException(ErrorEnum.MUST_LOGIN_WITH_GOOGLE);
        }

        // 💥 3. BẢO MẬT: Phải kiểm tra mật khẩu NGAY LẬP TỨC!
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!authenticated) {
            throw new AppException(ErrorEnum.UNAUTHENTICATED); // Sai mật khẩu thì đuổi ra ngay
        }

        // 💥 4. Mật khẩu ĐÚNG rồi mới bắt đầu phân nhánh trạng thái (Làm phẳng If-Else)
        if (Status.INACTIVE.equals(user.getStatus())) {
            // Chưa kích hoạt -> Ném lỗi kèm Email để FE bẻ lái sang trang nhập OTP
            Map<String, String> errorData = Map.of("email", user.getEmail());
            throw new AppException(ErrorEnum.USER_INACTIVE_OR_BLOCKED, errorData);

        } else if (!Status.ACTIVE.equals(user.getStatus())) {
            // Bị khóa/ban (BLOCKED...) -> Ném lỗi chung chung, cấm cửa
            throw new AppException(ErrorEnum.USER_INACTIVE_OR_BLOCKED);
        }

        // 5. Mọi thứ hoàn hảo -> Sinh Token
        var token = generateToken(user);

        log.info("✅ User '{}' đăng nhập thành công.", user.getUsername());

        return AuthenticationResponse.builder()
                .authenticated(true)
                .token(token)
                .isFirstTime(false)
                .build();
    }

    @Transactional
    public AuthenticationResponse googleLogin(GoogleLoginRequest request) {
        try {
            GoogleIdTokenVerifier googleVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                    .build();

            GoogleIdToken idToken = googleVerifier.verify(request.getCredential());

            if (idToken == null) {
                log.warn("Invalid ID token from Google.");
                throw new AppException(ErrorEnum.UNAUTHENTICATED);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            var userOptional = userRepository.findByEmail(email);

            // ==========================================
            // TRƯỜNG HỢP 1: USER CŨ ĐÃ TỒN TẠI
            // ==========================================
            if (userOptional.isPresent()) {
                User existingUser = userOptional.get();

                // 💥 Nâng cấp logic: Check xem user này đã điền đủ thông tin chưa?
                boolean isProfileIncomplete = existingUser.getJobTitle() == null || existingUser.getJobTitle().isEmpty()
                        || existingUser.getDob() == null
                        || existingUser.getAddress() == null || existingUser.getAddress().isEmpty();

                return AuthenticationResponse.builder()
                        .authenticated(true)
                        .token(generateToken(existingUser))
                        .isFirstTime(isProfileIncomplete) // Nếu thiếu thông tin, ép về true để bắt cập nhật
                        .build();
            }

            // ==========================================
            // TRƯỜNG HỢP 2: TẠO USER MỚI HOÀN TOÀN
            // ==========================================
            else {
                Role userRole = roleRepository.findByName(request.getRole())
                        .orElseThrow(() -> new AppException(ErrorEnum.ROLE_NOT_EXIST));

                String randomSuffix = java.util.UUID.randomUUID().toString().substring(0, 5);
                String generatedUsername = "google_" + email.split("@")[0] + "_" + randomSuffix;

                User newUser = User.builder()
                        .email(email)
                        .fullName(name)
                        .username(generatedUsername)
                        .password("googlelogin") // (Lưu ý: Nhớ chặn không cho đăng nhập tay bằng pass này ở hàm login thường nhé)
                        .avatarUrl(pictureUrl)
                        .status(Status.ACTIVE)
                        .jobTitle("")  // để trống chờ update
                        .authProvider(AuthProvider.GG)
                        .build();

                newUser.addRole(userRole);
                userRepository.save(newUser);

                if ("STUDENT".equals(request.getRole())) {
                    studentProfileRepository.save(StudentProfile.builder().user(newUser).build());
                } else if ("TEACHER".equals(request.getRole())) {
                    teacherProfileRepository.save(TeacherProfile.builder().user(newUser).build());
                }

                return AuthenticationResponse.builder()
                        .authenticated(true)
                        .token(generateToken(newUser))
                        .isFirstTime(true) // 100% là người mới
                        .build();
            }
        } catch (Exception e) {
            log.error("Error verifying Google token", e);
            throw new AppException(ErrorEnum.UNAUTHENTICATED);
        }
    }

    public IntrospectResponse introspect (IntrospectRequest introspectRequest) {
        boolean isValid = true;
        try{
            verifyToken(introspectRequest.getToken(), false);
        }
        catch (AppException e){
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    public void logout(LogoutRequest logoutRequest) {
        try {
            var signToken = verifyToken(logoutRequest.getToken(), true);
            String jti = signToken.getJWTClaimsSet().getJWTID();

            Date refreshExpiryTime = new Date(signToken.getJWTClaimsSet().getIssueTime().toInstant()
                    .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli());

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jti)
                    .expiryTime(refreshExpiryTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
            log.info("Đã đưa Token vào danh sách đen: {}", jti);

        } catch (AppException e) {
            log.info("Token đã hết hạn hoặc không hợp lệ. Mục đích đăng xuất đã hoàn thành.");
        } catch (ParseException e) {
            log.warn("Lỗi trích xuất thông tin Token khi đăng xuất");
        }
    }

    public AuthenticationResponse refreshToken(RefreshRequest request) {
        // 1. Verify token trong refresh window
        SignedJWT signedJWT = verifyToken(request.getToken(), true);

        try{
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            String jti = claims.getJWTID();
            Date refreshExpiryTime = new Date(claims.getIssueTime().toInstant()
                    .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli());

            // 2. Blacklist token cũ
            invalidatedTokenRepository.save(
                    InvalidatedToken.builder()
                            .id(jti)
                            .expiryTime(refreshExpiryTime)
                            .build()
            );

            // 3. Lấy user
            String username = claims.getSubject();
            User user = userRepository.findByUsernameWithRolesAndPermissions(username)
                    .orElseThrow(() -> new AppException(ErrorEnum.UNAUTHENTICATED));

            // 4. Tạo token mới
            String newToken = generateToken(user);

            return AuthenticationResponse.builder()
                    .authenticated(true)
                    .token(newToken)
                    .isFirstTime(false)
                    .build();

        } catch (ParseException e) {
            log.error("Lỗi trích xuất thông tin JWT", e);
            throw new AppException(ErrorEnum.UNAUTHENTICATED);
        }

    }

    private SignedJWT verifyToken (String token, boolean isRefresh)  {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            boolean verified = signedJWT.verify(verifier);

            Date expirationTime = (isRefresh)
                    ? new Date(signedJWT.getJWTClaimsSet().getIssueTime().toInstant()
                    .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli())
                    : signedJWT.getJWTClaimsSet().getExpirationTime();


            if (!verified || expirationTime == null || expirationTime.before(new Date())) {
                throw new AppException(ErrorEnum.UNAUTHENTICATED);
            }


            if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())) {
                throw new AppException(ErrorEnum.UNAUTHENTICATED);
            }

            return signedJWT;
        } catch (ParseException | JOSEException e){
            log.warn("Token không hợp lệ hoặc đã bị can thiệp");
            throw new AppException(ErrorEnum.UNAUTHENTICATED);
        }
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("echill.com")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS)))
                .jwtID(TsidCreator.getTsid().toString())
                .claim("scope", buildScope(user))
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(this.signer);
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Không thể tạo Token cho user: {}", user.getUsername(), e);
            throw new AppException(ErrorEnum.UNCATEGORIZED);
        }
    }

    private String buildScope(User user) {
        StringJoiner scope = new StringJoiner(" ");

        if (!CollectionUtils.isEmpty(user.getUserRoles())) {
            user.getUserRoles().forEach(userRole -> {
                // Tối ưu nhẹ: Đưa role ra một biến để code dễ đọc, đỡ phải gọi getRole() nhiều lần
                var role = userRole.getRole();

                scope.add("ROLE_" + role.getName());

                // Fix lỗi cú pháp: Code gốc của bạn đang bị thiếu 1 dấu đóng ngoặc ")" ở dòng if này
                if (!CollectionUtils.isEmpty(role.getRolePermissions())) {
                    role.getRolePermissions().forEach(rolePermission ->
                            scope.add(rolePermission.getPermission().getName())
                    );
                }
            });
        }
        return scope.toString();
    }

    private void validateOtpResendCooldown(String redisKey) {
        Long expireTime = redisTemplate.getExpire(redisKey);

        // Nếu OTP cũ vẫn còn sống và mới được tạo cách đây chưa đầy 60 giây (OTP_RESEND_COOLDOWN_SECONDS) -> Chặn
        if (expireTime != null && expireTime > OTP_EXPIRATION_MINUTES * 60 - OTP_RESEND_COOLDOWN_SECONDS) {
            throw new AppException(ErrorEnum.PLEASE_WAIT_BEFORE_RESEND);
        }
    }

    private void generateAndSendOtp (String email, String fullName, boolean isForgot) {
        // Sinh số ngẫu nhiên từ 0 đến 999999
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        log.info("Mã otp vừa sinh: {}", otp);

        // 💥 Dùng toán tử 3 ngôi để code gọn và sạch hơn
        String redisKey = isForgot ? RESET_OTP_PREFIX + email : OTP_PREFIX + email;

        // Lưu vào Redis với TTL 5 phút
        redisTemplate.opsForValue().set(redisKey, otp, Duration.ofMinutes(OTP_EXPIRATION_MINUTES));

        // Phân luồng gọi EmailService
        if (isForgot) {
            emailService.sendForgotPasswordEmailAsync(email, fullName, otp);
        } else {
            emailService.sendOtpEmailAsync(email, fullName, otp);
        }
    }

    private void validateOtp(String redisKey, String inputOtp) {
        String savedOtp = redisTemplate.opsForValue().get(redisKey);

        // Trường hợp 1: OTP không tồn tại hoặc đã quá 5 phút (Redis đã tự xoá)
        if (savedOtp == null) {
            throw new AppException(ErrorEnum.OTP_EXPIRED);
        }

        // Trường hợp 2: Sai mã OTP
        if (!savedOtp.equals(inputOtp)) {
            throw new AppException(ErrorEnum.OTP_INCORRECT);
        }
    }

}
