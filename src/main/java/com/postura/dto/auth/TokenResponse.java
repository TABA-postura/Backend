package com.postura.dto.auth;

import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 성공 후 서버가 클라이언트에게 발급하는 JWT 토큰 정보를 담습니다.
 */
@Getter
@Builder
public class TokenResponse {

    private final String accessToken;   // 실제 리소스 접근에 사용되는 토큰
    private final String refreshToken;  // AccessToken 만료 시 재발급에 사용되는 토큰

    /**
     * tokenType: 인증 요청 시 사용될 토큰의 타입 (JWT 표준: "Bearer")
     */
    @Builder.Default
    private final String tokenType = "Bearer";
}
