package com.postura.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter // getRefreshToken() 메서드를 자동 생성합니다.
@Setter // @RequestBody로 JSON 데이터를 객체에 바인딩하기 위해 필요합니다.
@NoArgsConstructor // JSON 라이브러리가 객체를 생성할 때 필요한 기본 생성자를 추가합니다.
public class RefreshTokenRequest {

    // 클라이언트가 서버로 보내는 Refresh Token 문자열입니다.
    // @NotBlank: 필드가 null이거나 비어있지 않도록 검증합니다. (jakarta.validation)
    @NotBlank(message = "Refresh Token은 필수 값입니다.")
    private String refreshToken;
}