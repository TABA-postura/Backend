package com.postura.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 BAD_REQUEST: 잘못된 요청
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력 값입니다."),
    INVALID_SESSION_STATUS(HttpStatus.BAD_REQUEST, "M004", "세션 상태 변경이 불가능합니다."),

    // 401 UNAUTHORIZED: 인증 실패
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증에 실패했습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "A002", "비밀번호가 일치하지 않습니다."),

    // 404 NOT_FOUND: 리소스를 찾을 수 없음
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "M003", "활성 세션을 찾을 수 없습니다."),

    // 500 INTERNAL_SERVER_ERROR: 서버 내부 오류
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status; // HTTP 상태 코드 (400, 404, 500 등)
    private final String code;       // 프로젝트 내 고유 오류 코드 (U001, M003 등)
    private final String message;    // 사용자에게 보여줄 메시지
}