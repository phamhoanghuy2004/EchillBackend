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
    TEST_SET_EXISTED(2021, "Lesson already has a TestSet", HttpStatus.BAD_REQUEST),
    INVALID_CORRECT_ANSWER(2022, "Correct answer must be A, B, C, or D", HttpStatus.BAD_REQUEST),
    MISSING_CORRECT_ANSWER_COLUMN(2023, "Correct answer column is missing or empty", HttpStatus.BAD_REQUEST),
    INVALID_SKILL_TYPE(2024, "Invalid skill type: {skill}", HttpStatus.BAD_REQUEST),
    EXCEL_PARSE_ERROR(2025, "Error parsing Excel file", HttpStatus.BAD_REQUEST),
    TEST_SET_NOT_FOUND(2026, "TestSet not found", HttpStatus.NOT_FOUND),
    TEST_NOT_FOUND(2027, "Test not found", HttpStatus.NOT_FOUND),
    TAG_NOT_FOUND(2028, "Tag not found", HttpStatus.NOT_FOUND),
    FILE_EMPTY(2029, "File must not be empty", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE(2030, "File type invalid", HttpStatus.BAD_REQUEST),
    INVALID_DURATION(2031, "Duration must be greater than 0", HttpStatus.BAD_REQUEST),
    INVALID_PASS_SCORE(2032, "Pass score must be between 0 and 100", HttpStatus.BAD_REQUEST),
    QUESTION_NOT_FOUND(2033, "Question not found", HttpStatus.NOT_FOUND),
    TEST_TITLE_REQUIRED(2034, "Test title must not be blank", HttpStatus.BAD_REQUEST),
    DURATION_REQUIRED(2035, "Duration is required", HttpStatus.BAD_REQUEST),
    PASS_SCORE_REQUIRED(2036, "Pass score is required", HttpStatus.BAD_REQUEST),
    TEST_SET_ID_REQUIRED(2037, "Test set ID is required", HttpStatus.BAD_REQUEST),
    ;

    Integer code;
    String message;
    HttpStatus statusCode;
}
