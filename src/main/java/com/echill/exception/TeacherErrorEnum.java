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

    TITLE_REQUIRED(2101, "Lesson title must not be blank", HttpStatus.BAD_REQUEST),
    TITLE_TOO_LONG(2102, "Lesson title must not exceed 200 characters", HttpStatus.BAD_REQUEST),
    CONTENT_REQUIRED(2103, "Lesson content must not be blank", HttpStatus.BAD_REQUEST),
    DISPLAY_ORDER_REQUIRED(2104, "Display order must not be null", HttpStatus.BAD_REQUEST),
    DISPLAY_ORDER_INVALID(2105, "Display order must be greater than or equal to 0", HttpStatus.BAD_REQUEST),
    PREVIEW_REQUIRED(2106, "Preview status must not be null", HttpStatus.BAD_REQUEST)
    ;

    Integer code;
    String message;
    HttpStatus statusCode;
}
