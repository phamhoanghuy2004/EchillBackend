package com.echill.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorEnum {
    UNCATEGORIZED(9999, "Uncategorized", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.INTERNAL_SERVER_ERROR),
    USERNAME_EXISTED(1002, "Username is already in use", HttpStatus.BAD_REQUEST),

    // TỐI ƯU: Đổi cứng số 4 và 8 thành tham số {min} để dễ dàng thay đổi ở DTO mà không cần sửa Enum
    INVALID_USERNAME(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),

    USER_NOTFOUND(1005, "User not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
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
    ;


    Integer code;
    String message;
    HttpStatus statusCode;
}
