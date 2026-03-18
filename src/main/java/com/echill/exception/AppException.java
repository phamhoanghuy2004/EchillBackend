package com.echill.exception;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppException extends RuntimeException {
    ErrorEnum errorEnum;
    @NonFinal
    Object data;

    public AppException(ErrorEnum errorEnum) {
        super(errorEnum.getMessage());
        this.errorEnum = errorEnum;
    }

    public AppException(ErrorEnum errorEnum, Object data) {
        super(errorEnum.getMessage());
        this.errorEnum = errorEnum;
        this.data = data;
    }
}
