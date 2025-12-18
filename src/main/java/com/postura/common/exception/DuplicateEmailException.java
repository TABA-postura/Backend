package com.postura.common.exception;

/**
 * 회원가입 시 이메일 중복 케이스
 */
public class DuplicateEmailException extends RuntimeException {

    private final String email;

    public DuplicateEmailException(String email) {
        super("이미 가입된 이메일입니다.");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
