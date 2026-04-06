package com.echill.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum StudentErrorEnum implements ErrorCode {
    // ==========================================
    // CÁC MÃ LỖI MỚI CHO STUDY GOAL
    // ==========================================
    CERT_TYPE_REQUIRED(1045, "Certificate type cannot be blank", HttpStatus.BAD_REQUEST),
    LISTENING_SCORE_INVALID(1046, "Listening score must be between {min} and {max}", HttpStatus.BAD_REQUEST),
    READING_SCORE_INVALID(1047, "Reading score must be between {min} and {max}", HttpStatus.BAD_REQUEST),
    SPEAKING_SCORE_INVALID(1048, "Speaking score must be between {min} and {max}", HttpStatus.BAD_REQUEST),
    WRITING_SCORE_INVALID(1049, "Writing score must be between {min} and {max}", HttpStatus.BAD_REQUEST),
    TOTAL_SCORE_INVALID(1050, "Total score is invalid", HttpStatus.BAD_REQUEST),
    PROFILE_NOT_FOUND(1051, "Student profile not found. Please complete your profile first.", HttpStatus.NOT_FOUND),
    SCORE_REQUIRED(1052, "Score values cannot be null", HttpStatus.BAD_REQUEST),
    GOAL_NOT_FOUND(1053, "Study goal not found or already inactive", HttpStatus.NOT_FOUND),
    ALREADY_OWNED_COURSE(1054, "You already own this course", HttpStatus.CONFLICT),
    ;
    Integer code;
    String message;
    HttpStatus statusCode;
}
