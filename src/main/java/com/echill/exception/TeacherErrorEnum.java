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
    ;

    Integer code;
    String message;
    HttpStatus statusCode;
}
