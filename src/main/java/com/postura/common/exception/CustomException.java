package com.postura.common.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    // 1. ErrorCode만 받는 생성자 (가장 일반적인 사용)
    public CustomException(ErrorCode errorCode) {
        // RuntimeException의 메시지로 ErrorCode의 메시지 사용
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 2. 메시지를 커스텀 할 수 있는 생성자 (특정 파라미터 정보 포함 시)
    public CustomException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + " [" + detail + "]");
        this.errorCode = errorCode;
    }
}
