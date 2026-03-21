package com.echill.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorEnum implements ErrorCode {

    UNCATEGORIZED(9999, "Uncategorized", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.INTERNAL_SERVER_ERROR),
    USERNAME_EXISTED(1002, "Username is already in use, please login", HttpStatus.BAD_REQUEST),

    // TỐI ƯU: Đổi cứng số 4 và 8 thành tham số {min} để dễ dàng thay đổi ở DTO mà không cần sửa Enum
    INVALID_USERNAME(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),

    USER_NOTFOUND(1005, "User not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated, please login!", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission to perform this action", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "You must be at least {min} years old.", HttpStatus.BAD_REQUEST),

    // CÁC MÃ LỖI MỚI THÊM CHO VALIDATION DTO
    USERNAME_REQUIRED(1009, "Username cannot be blank", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(1010, "Password cannot be blank", HttpStatus.BAD_REQUEST),
    TOKEN_REQUIRED(1011, "Token cannot be blank", HttpStatus.BAD_REQUEST),
    PERMISSION_NAME_REQUIRED(1012, "Permission name cannot be blank", HttpStatus.BAD_REQUEST),
    PERMISSION_EXISTED(1013, "Permission already exists", HttpStatus.BAD_REQUEST),
    PERMISSION_NOT_EXIST(1014, "Permission does not exist", HttpStatus.BAD_REQUEST),
    ROLE_NOT_EXIST(1015, "Role does not exist", HttpStatus.BAD_REQUEST),
    ROLE_NAME_REQUIRED(1016, "Role name cannot be blank", HttpStatus.BAD_REQUEST),
    ROLE_EXISTED(1017, "Role already exists", HttpStatus.BAD_REQUEST),

    // ==========================================
    // CÁC MÃ LỖI MỚI CHO STUDENT REGISTER DTO
    // ==========================================
    USERNAME_TOO_LONG(1018, "Username must not exceed {max} characters", HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED(1019, "Email cannot be blank", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID_FORMAT(1020, "Invalid email format", HttpStatus.BAD_REQUEST),
    EMAIL_TOO_LONG(1021, "Email must not exceed {max} characters", HttpStatus.BAD_REQUEST),
    PASSWORD_LENGTH_INVALID(1022, "Password must be between {min} and {max} characters", HttpStatus.BAD_REQUEST),
    PASSWORD_FORMAT_INVALID(1023, "Password must contain at least one uppercase letter, one lowercase letter, and one number", HttpStatus.BAD_REQUEST),
    FULL_NAME_REQUIRED(1024, "Full name cannot be blank", HttpStatus.BAD_REQUEST),
    FULL_NAME_TOO_LONG(1025, "Full name must not exceed {max} characters", HttpStatus.BAD_REQUEST),
    DOB_INVALID_PAST(1026, "Date of birth must be in the past", HttpStatus.BAD_REQUEST),
    ADDRESS_TOO_LONG(1027, "Address must not exceed {max} characters", HttpStatus.BAD_REQUEST),
    JOB_TITLE_REQUIRED(1028, "Job title cannot be blank", HttpStatus.BAD_REQUEST),
    JOB_TITLE_TOO_LONG(1029, "Job title must not exceed {max} characters", HttpStatus.BAD_REQUEST),
    AVATAR_URL_INVALID(1030, "Avatar URL is invalid", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(1031, "Email already exists, please login", HttpStatus.CONFLICT),
    USER_ALREADY_ACTIVE_OR_BLOCKED(1032,  "User already active or blocked", HttpStatus.CONFLICT),
    PLEASE_WAIT_BEFORE_RESEND(1033, "Please wait before resend", HttpStatus.BAD_REQUEST),
    OTP_REQUIRED(1034, "OTP cannot be blank", HttpStatus.BAD_REQUEST),
    OTP_INVALID_LENGTH(1035, "OTP must be between {min} and {max} characters", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1036, "OTP expired", HttpStatus.BAD_REQUEST),
    OTP_INCORRECT(1037, "OTP incorrect", HttpStatus.BAD_REQUEST),
    USER_INACTIVE_OR_BLOCKED(1038, "User inactive or blocked", HttpStatus.BAD_REQUEST),
    MUST_LOGIN_WITH_GOOGLE(1039, "You must login with your GG account", HttpStatus.BAD_REQUEST),
    ROLE_REQUIRED(1040, "Role cannot be blank", HttpStatus.BAD_REQUEST),
    ROLE_INVALID(1041, "Role does not exist", HttpStatus.BAD_REQUEST),
    CANNOT_UPLOAD_IMAGE(1042, "Unable to upload image. Please remove your current image and try uploading again.", HttpStatus.BAD_REQUEST),
    INVALID_AVATAR_FORMAT(1043, "Invalid image format. Please upload a supported image type (e.g., JPG, PNG).", HttpStatus.BAD_REQUEST),
    AVATAR_SIZE_TOO_LARGE(1044, "Image size exceeds the allowed limit. Please upload a smaller image.", HttpStatus.BAD_REQUEST)
    ;


    Integer code;
    String message;
    HttpStatus statusCode;
}
