package com.echill.exception;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE,  makeFinal = true)
public class AppException extends RuntimeException {
    ErrorEnum errorEnum;
    public AppException(ErrorEnum errorEnum) {
        super(errorEnum.getMessage());
        this.errorEnum = errorEnum;
    }
}
