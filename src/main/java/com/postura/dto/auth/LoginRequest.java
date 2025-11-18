package com.postura.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로그인 시 필요한 이메일(ID)과 비밀번호 정보를 담습니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    // 이메일(ID)은 필수 입력 값이며, 이메일 형식(@ 포함)을 검증합니다.
    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식에 맞지 않습니다.")
    private String email;

    // 비밀번호는 필수 입력 값입니다.
    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    private String password;
}
