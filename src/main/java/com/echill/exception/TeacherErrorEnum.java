package com.echill.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum TeacherErrorEnum implements ErrorCode {
    BLOG_NOT_FOUND(2001, "Blog not found", HttpStatus.BAD_REQUEST),
    UPLOAD_IMAGE_FAILED(2001, "Up load image failed", HttpStatus.BAD_REQUEST),
    COURSE_NOT_FOUND(2002, "Course not found", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND(2003, "Category not found", HttpStatus.BAD_REQUEST),
    COURSE_ID_REQUIRED(2100, "Course ID must not be null", HttpStatus.BAD_REQUEST),

    LESSON_TITLE_REQUIRED(2101, "Lesson title must not be blank", HttpStatus.BAD_REQUEST),
    LESSON_TITLE_TOO_LONG(2102, "Lesson title must not exceed 200 characters", HttpStatus.BAD_REQUEST),
    LESSON_CONTENT_REQUIRED(2103, "Lesson content must not be blank", HttpStatus.BAD_REQUEST),
    LESSON_DISPLAY_ORDER_REQUIRED(2104, "Display order must not be null", HttpStatus.BAD_REQUEST),
    LESSON_DISPLAY_ORDER_INVALID(2105, "Display order must be greater than or equal to 0", HttpStatus.BAD_REQUEST),
    LESSON_PREVIEW_REQUIRED(2106, "Preview status must not be null", HttpStatus.BAD_REQUEST),

    BLOG_TITLE_REQUIRED(2107, "Blog title must not be blank", HttpStatus.BAD_REQUEST),
    BLOG_CONTENT_REQUIRED(2018, "Blog content must not be blank", HttpStatus.BAD_REQUEST),
    PROFILE_NOT_FOUND(2019, "Teacher profile not found. Please complete your profile first.", HttpStatus.NOT_FOUND),
    CERTIFICATE_REQUIRED(2020, "Certificate must not be null", HttpStatus.NOT_FOUND),
    ;

    Integer code;
    String message;
    HttpStatus statusCode;
}
