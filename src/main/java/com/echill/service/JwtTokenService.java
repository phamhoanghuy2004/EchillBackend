package com.echill.service;

import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.repository.InvalidatedTokenRepository;
import com.github.f4b6a3.tsid.TsidCreator;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class JwtTokenService {
    @NonFinal
    JWSVerifier verifier;

    @NonFinal
    JWSSigner signer;

    @NonFinal
    @Value("${jwt.signerKey}")
    String SIGNER_KEY;

    @Getter
    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;


    InvalidatedTokenRepository invalidatedTokenRepository;

    @PostConstruct
    public void init() throws JOSEException {
        byte[] sharedKey = SIGNER_KEY.getBytes(StandardCharsets.UTF_8);
        this.verifier = new MACVerifier(sharedKey);
        this.signer = new MACSigner(sharedKey);
    }

    public SignedJWT verifyToken (String token, boolean isRefresh)  {
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

    public String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("echill.com")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS)))
                .jwtID(TsidCreator.getTsid().toString())
                .claim("scope", buildScope(user))
                .claim("userId", user.getId())
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

    public String buildScope(User user) {
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
