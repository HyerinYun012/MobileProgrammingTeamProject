package com.petplace.exception;

import lombok.Getter;

/**
 * 프로젝트 전반에서 발생하는 비즈니스 로직 예외를 처리하기 위한 커스텀 예외 클래스
 */
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}