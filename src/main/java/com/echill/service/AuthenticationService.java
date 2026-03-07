package com.echill.service;

import com.echill.dto.request.AuthenticationRequest;
import com.echill.dto.request.IntrospectRequest;
import com.echill.dto.request.LogoutRequest;
import com.echill.dto.request.RefreshRequest;
import com.echill.dto.response.AuthenticationResponse;
import com.echill.dto.response.IntrospectResponse;
import com.echill.entity.InvalidatedToken;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.InvalidatedTokenRepository;
import com.echill.repository.UserRepository;
import com.github.f4b6a3.tsid.TsidCreator;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
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

    InvalidatedTokenRepository invalidatedTokenRepository;

    UserRepository userRepository;

    PasswordEncoder passwordEncoder;

    public AuthenticationResponse authenticate (AuthenticationRequest request) {
        var user = userRepository.findByUsernameWithRolesAndPermissions(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        // TỐI ƯU 2: Sử dụng thẳng passwordEncoder đã được tiêm
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) {
            throw new AppException(ErrorEnum.UNAUTHENTICATED);
        }

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .authenticated(true)
                .token(token)
                .build();
    }

    public IntrospectResponse introspect (IntrospectRequest introspectRequest) {
        var token = introspectRequest.getToken();
        boolean isValid = true;
        try{
            verifyToken(token, false);
        }
        catch (Exception e){
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    public void logout(LogoutRequest logoutRequest) {
        try {
            var signToken = verifyToken(logoutRequest.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken =
                    InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

            invalidatedTokenRepository.save(invalidatedToken);
            log.info("Đã đưa Token vào danh sách đen: {}", jit);

        } catch (AppException e) {
            // TRƯỜNG HỢP 1: Token ĐÃ HẾT HẠN hoặc ĐÃ BỊ LOGOUT TỪ TRƯỚC
            // Mục đích của người dùng là muốn hủy Token. Nếu nó đã hỏng sẵn rồi -> Quá tốt!
            // Không quăng lỗi, chỉ ghi log và xem như Đăng xuất thành công.
            log.info("Token đã hết hạn hoặc đã vô hiệu hóa. Bỏ qua bước đưa vào Blacklist.");

        } catch (JOSEException | ParseException e) {
            // TRƯỜNG HỢP 2: Hacker gửi 1 chuỗi Token bịa đặt (sai định dạng, fake chữ ký)
            // Không thèm xử lý, lờ đi luôn để bảo vệ Database khỏi các request rác.
            log.warn("Phát hiện Token sai định dạng gửi vào luồng Logout!");
        }
    }

    public AuthenticationResponse refreshToken(RefreshRequest request)
            throws ParseException, JOSEException {

        String token = request.getToken();
        if (token == null || token.isBlank()) {
            throw new AppException(ErrorEnum.UNAUTHENTICATED);
        }

        // 1. Verify token trong refresh window
        SignedJWT signedJWT = verifyToken(token, true);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        String jti = claims.getJWTID();
        Date expiryTime = claims.getExpirationTime();

        // 2. Blacklist token cũ
        invalidatedTokenRepository.save(
                InvalidatedToken.builder()
                        .id(jti)
                        .expiryTime(expiryTime)
                        .build()
        );

        // 3. Lấy user
        String username = claims.getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorEnum.UNAUTHENTICATED));

        // 4. Tạo token mới
        String newToken = generateToken(user);

        return AuthenticationResponse.builder()
                .authenticated(true)
                .token(newToken)
                .build();
    }

    private SignedJWT verifyToken (String token, boolean isRefresh) throws JOSEException, ParseException {

        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes(StandardCharsets.UTF_8));

        SignedJWT signedJWT = SignedJWT.parse(token);
        var verified = signedJWT.verify(verifier);
        Date expirationTime = (isRefresh)
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime()
                .toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();


        if (!verified || expirationTime == null || expirationTime.before(new Date())) {
            throw new AppException(ErrorEnum.UNAUTHENTICATED);
        }


        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())) {
            throw new AppException(ErrorEnum.UNAUTHENTICATED);
        }

        return signedJWT;
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);


        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("echill.com") // Chuẩn quy ước URL thường viết thường
                .issueTime(new Date())
                // TỐI ƯU 1: Viết gọn lại logic cộng thời gian
                .expirationTime(Date.from(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS)))
                // TỐI ƯU 2: Nhắc lại - Dùng TSID thay cho UUID để Database truy vấn nhanh như chớp!
                .jwtID(TsidCreator.getTsid().toString())
                .claim("scope", buildScope(user))
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            // TỐI ƯU 3: Cắm chặt UTF-8 để chống lỗi môi trường OS
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes(StandardCharsets.UTF_8)));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            // TỐI ƯU 4: Dùng Log và ném AppException chuẩn của hệ thống
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

}
