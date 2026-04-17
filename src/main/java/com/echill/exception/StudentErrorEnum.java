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
    MAX_ATTEMPT_REACHED(1055, "Max attempt reached", HttpStatus.BAD_REQUEST),
    TEST_NOT_FOUND(1056, "Test not found", HttpStatus.NOT_FOUND),
    SESSION_EXPIRED_MUST_SUBMIT(1057, "Session expired must submit", HttpStatus.BAD_REQUEST),
    SESSION_ID_REQUIRED(1058, "Session id cannot be null", HttpStatus.BAD_REQUEST),
    ALREADY_SUBMITTED(1057, "Already Submitted", HttpStatus.BAD_REQUEST),
    SESSION_NOT_FOUND(1058, "Session not found", HttpStatus.NOT_FOUND),
    PAYLOAD_TOO_LARGE(1059, "Payload too large", HttpStatus.BAD_REQUEST),
    TEST_RESULT_NOT_FOUND(1060, "Test result not found", HttpStatus.NOT_FOUND),
    NOT_ENROLLED(1061, "Not enrolled", HttpStatus.BAD_REQUEST),
    COURSE_LOCKED(1062, "Course locked", HttpStatus.BAD_REQUEST),
    PREVIOUS_LESSON_NOT_COMPLETED(1063, "Previous lesson is not completed", HttpStatus.BAD_REQUEST),
    LESSON_NOT_STARTED(1064, "Lesson is not started", HttpStatus.BAD_REQUEST),
    LESSON_NOT_READY(1065, "Lesson is not ready", HttpStatus.BAD_REQUEST),
    NOT_ENOUGH_PROGRESS(1066, "Not enough progress to complete this course", HttpStatus.BAD_REQUEST),

    ;
    Integer code;
    String message;
    HttpStatus statusCode;
}
