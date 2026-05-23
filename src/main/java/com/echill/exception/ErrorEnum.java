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
    INVALID_IMAGE_FORMAT(1043, "Invalid image format. Please upload a supported image type (e.g., JPG, PNG).", HttpStatus.BAD_REQUEST),
    IMAGE_SIZE_TOO_LARGE(1044, "Image size exceeds the allowed limit. Please upload a smaller image.", HttpStatus.BAD_REQUEST),
    RATE_LIMIT_EXCEEDED(1045, "You have reached rate limit", HttpStatus.TOO_MANY_REQUESTS),
    LESSON_NOT_FOUND(1046, "Lesson not found", HttpStatus.BAD_REQUEST),
    DATABASE_ERROR(1047, "Database error, please try again!", HttpStatus.INTERNAL_SERVER_ERROR),
    DATA_CONFLICT(1048, "The data has been modified by another process. Please refresh the page and try again.", HttpStatus.CONFLICT),
    VIDEO_IS_PROCESSING(1049, "Video is processing", HttpStatus.BAD_REQUEST),
    UPLOAD_AVT_FAILED(1050, "Upload avatar failed", HttpStatus.BAD_REQUEST),
    INVALID_FILE_FORMAT(1051, "Invalid file format. Please upload PDF or Word documents.", HttpStatus.BAD_REQUEST),
    FILE_SIZE_TOO_LARGE(1052, "File size exceeds the allowed limit (10MB).", HttpStatus.BAD_REQUEST),
    CANNOT_UPLOAD_FILE(1053, "Unable to upload file. Please try again.", HttpStatus.BAD_REQUEST),
    DOCUMENT_NOT_FOUND(1054, "Document not found", HttpStatus.BAD_REQUEST),
    DOCUMENT_REQUIRED(1055, "Document required", HttpStatus.BAD_REQUEST),
    CERTIFICATE_NOT_FOUND(1056, "Certificate not found", HttpStatus.NOT_FOUND),
    CERT_TYPE_REQUIRED(1057, "Certificate type is required", HttpStatus.BAD_REQUEST),
    ISSUED_DATE_REQUIRED(1058, "Issued date is required", HttpStatus.BAD_REQUEST),

    // ==========================================
    // CÁC MÃ LỖI CHO VNPAY IPN WEBHOOK
    // ==========================================
    VNPAY_ORDER_NOT_FOUND(1059, "Order not found", HttpStatus.BAD_REQUEST),
    VNPAY_ORDER_ALREADY_CONFIRMED(1060, "Order already confirmed", HttpStatus.BAD_REQUEST),
    VNPAY_INVALID_AMOUNT(1061, "Invalid amount", HttpStatus.BAD_REQUEST),
    VNPAY_INVALID_SIGNATURE(1062, "Invalid signature", HttpStatus.BAD_REQUEST),
    VNPAY_SIGNATURE_GENERATION_FAILED(1063, "Failed to generate signature", HttpStatus.INTERNAL_SERVER_ERROR),
    CAN_NOT_BUILD_QUERY_URL(1064, "Cannot build query URL", HttpStatus.BAD_REQUEST),
    TRANSACTION_NOT_FOUND(1065, "Transaction not found", HttpStatus.BAD_REQUEST),
    INVALID_AMOUNT(1066, "Invalid amount", HttpStatus.BAD_REQUEST),
    TRANSACTION_INVALID_STATUS(1067, "Invalid transaction status", HttpStatus.BAD_REQUEST),
    INVALID_VIDEO_DURATION(1068, "Invalid video duration", HttpStatus.BAD_REQUEST),
    VOUCHER_DATE_INVALID(1069, "Voucher date is invalid", HttpStatus.BAD_REQUEST),
    VOUCHER_PERCENT_INVALID(1070, "Voucher percent is invalid", HttpStatus.BAD_REQUEST),
    VOUCHER_CODE_EXISTED(1071, "Voucher code existed", HttpStatus.BAD_REQUEST),
    VOUCHER_MAX_DISCOUNT_REQUIRED(1072, "Voucher max discount is required", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_FOUND(1073, "Voucher not found", HttpStatus.BAD_REQUEST),
    VOUCHER_ALREADY_USED(1074, "Voucher already used", HttpStatus.BAD_REQUEST),
    VOUCHER_CONDITION_NOT_MET(1075, "Not eligible to apply voucher", HttpStatus.BAD_REQUEST),
    VOUCHER_USAGE_LIMIT_EXCEEDED(1076, "Voucher usage limit exceeded", HttpStatus.BAD_REQUEST),
    COIN_PACKAGE_NAME_ALREADY_EXISTS(1077, "Coin package name already exists", HttpStatus.BAD_REQUEST),
    ORIGINAL_PRICE_LESS_THAN_SALE_PRICE(1078, "Original price cannot be less than sale price", HttpStatus.BAD_REQUEST),
    COIN_PACKAGE_NOT_FOUND(1079, "Coin package not found", HttpStatus.NOT_FOUND),
    INVALID_COIN_AMOUNT(1080, "Invalid coin amount", HttpStatus.BAD_REQUEST),
    REWARD_ALREADY_CLAIMED(1081, "Reward already claimed", HttpStatus.BAD_REQUEST),
    NOT_ELIGIBLE_FOR_REWARD(1082, "Not eligible for reward", HttpStatus.BAD_REQUEST),
    COURSE_NOT_FOUND(1083, "Course not found", HttpStatus.NOT_FOUND),
    NOT_ENROLLED(1084, "You are not enrolled in this course", HttpStatus.BAD_REQUEST),
    REVIEW_NOT_FOUND(1085, "Review not found", HttpStatus.NOT_FOUND),
    CONSULTATION_ALREADY_CLAIMED (1086, "Consultation already claimed", HttpStatus.BAD_REQUEST),
    CONSULTATION_NOT_FOUND(1087, "Consultation not found", HttpStatus.NOT_FOUND),
    NOT_ENOUGH_FULL_TESTS(1088, "Not enough full tests", HttpStatus.BAD_REQUEST),
    USER_ALREADY_BLOCKED(1088, "User already blocked", HttpStatus.BAD_REQUEST),
    CANNOT_BLOCK_ADMIN(1089, "Cannot block an admin user", HttpStatus.BAD_REQUEST),
    PLACEMENT_TEST_ALREADY_COMPLETED(
            1090,
            "Bạn đã hoàn thành bài đánh giá đầu vào. Vui lòng tiếp tục lộ trình học của bạn!",
            HttpStatus.BAD_REQUEST
    ),
    SYSTEM_PARENT_TAG_NOT_CONFIGURED(
            1091,
            "Hệ thống chưa có Tag Cha nào được cấu hình!",
            HttpStatus.INTERNAL_SERVER_ERROR
    ),
    REQUEST_PROCESSING_TOO_FAST(
            1092,
            "Hệ thống đang xử lý câu trả lời của bạn, vui lòng không thao tác quá nhanh!",
            HttpStatus.TOO_MANY_REQUESTS
    ),
    QUESTION_ALREADY_PROCESSED(
            1093,
            "Câu hỏi này đã được chấm điểm! Vui lòng làm mới trang (F5) để tiếp tục.",
            HttpStatus.CONFLICT
    ),
    INVALID_QUESTION_ID(
            1094,
            "Phát hiện bất thường: ID câu hỏi không hợp lệ!",
            HttpStatus.BAD_REQUEST
    ),
    QUESTION_NOT_FOUND_OR_CACHE_EXPIRED(
            1095,
            "Câu hỏi không tồn tại hoặc Cache đã bị xóa!",
            HttpStatus.NOT_FOUND
    ),
    ;


    Integer code;
    String message;
    HttpStatus statusCode;
}
