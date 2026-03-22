package com.echill.exception;

import com.echill.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
@Slf4j

public class GlobalExceptionHandler {
    private static final Map<String, ErrorCode> ERROR_CODE_MAP = new HashMap<>();

    // Khối static này chạy duy nhất 1 lần khi Spring Boot khởi động
    static {
        // Quét tất cả các lỗi của hệ thống chung
        for (ErrorEnum e : ErrorEnum.values()) {
            ERROR_CODE_MAP.put(e.name(), e);
        }
        // Quét tất cả các lỗi của Student
        for (StudentErrorEnum e : StudentErrorEnum.values()) {
            ERROR_CODE_MAP.put(e.name(), e);
        }
        // Sau này có TeacherErrorEnum thì cứ copy thêm 1 vòng for vào đây là xong!
    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<Void>> handleRuntimeException(Exception ex) {
        // TỐI ƯU 2: Phải log ra toàn bộ Stack Trace để Dev còn biết đường fix bug
        log.error("Uncategorized Exception occurred: ", ex);

        var errorEnum = ErrorEnum.UNCATEGORIZED;
        return ResponseEntity.status(errorEnum.getStatusCode()).body(
                ApiResponse.<Void>builder()
                        .code(errorEnum.getCode())
                        .message(errorEnum.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {

        // Mặc định fallback nếu không tìm thấy key
        ErrorCode errorCode = ErrorEnum.INVALID_KEY;
        Map<String, Object> attributes = null;

        try {
            // Lấy key từ annotation (Ví dụ: "LISTENING_SCORE_INVALID")
            String enumKey = Objects.requireNonNull(ex.getFieldError()).getDefaultMessage();

            // 💥 TÌM TRONG KHO CHỨA (Chỉ 1 dòng, không cần try-catch valueOf nữa)
            if (ERROR_CODE_MAP.containsKey(enumKey)) {
                errorCode = ERROR_CODE_MAP.get(enumKey);
            } else {
                log.warn("Validation message key không tồn tại trong hệ thống: {}", enumKey);
            }

            var constraintViolation = ex.getBindingResult()
                    .getAllErrors()
                    .getFirst()
                    .unwrap(ConstraintViolation.class);

            attributes = constraintViolation.getConstraintDescriptor().getAttributes();

        } catch (Exception e) {
            log.error("Có lỗi không xác định khi bóc tách Validation Exception", e);
        }

        // Map các biến {min}, {max} (nếu có) vào chuỗi thông báo
        String finalMessage = Objects.nonNull(attributes) ? mapAttribute(errorCode.getMessage(), attributes) : errorCode.getMessage();

        return ResponseEntity.status(errorCode.getStatusCode()).body(
                ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(finalMessage)
                        .build()
        );
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<?>> handleAppException(AppException ex) {
        var errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatusCode()).body(
                ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .data(ex.getData())
                        .build()
        );
    }

    @ExceptionHandler(value = AuthorizationDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handleAuthorizationException(AuthorizationDeniedException ex) {
        var errorEnum = ErrorEnum.UNAUTHORIZED;
        log.warn("Access denied: {}", ex.getMessage()); // Nên log lại để trace xem ai cố tình truy cập trái phép
        return ResponseEntity.status(errorEnum.getStatusCode()).body(
                ApiResponse.<Void>builder()
                        .code(errorEnum.getCode())
                        .message(errorEnum.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorEnum errorEnum = ErrorEnum.UNAUTHORIZED;
        return ResponseEntity.status(errorEnum.getStatusCode()).body(
                ApiResponse.<Void>builder()
                        .code(errorEnum.getCode())
                        .message(errorEnum.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlingNoResourceFoundException(NoResourceFoundException exception) {

        // Trả về HTTP Status 404 (Not Found) kèm thông báo rõ ràng
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder()
                        .code(404)
                        .message("Endpoint does not exist: " + exception.getResourcePath())
                        .build());
    }

    // TỐI ƯU 3: Hàm "Ma thuật" tự động thay thế TẤT CẢ các parameter ({min}, {max}, {value}...)
    private String mapAttribute(String message, Map<String, Object> attributes) {
        String result = message;
        // Duyệt qua toàn bộ attributes của Validator, thấy chữ nào bọc trong ngoặc nhọn {} thì đè dữ liệu vào
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        return result;
    }
}
