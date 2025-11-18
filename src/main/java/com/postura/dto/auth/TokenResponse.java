package com.postura.dto.auth;


import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 성공 후 서버가 클라이언트에게 발급하는 JWT 토큰 정보를 담습니다.
 */
@Getter
@Builder // Builder 패턴을 사용하여 객체 생성
public class TokenResponse {

    private String accessToken; // 실제 리소스 접근에 사용되는 토큰

    private String refreshToken; // 엑세스 토큰 만료 시 재발급에 사용되는 토큰

    /**
     * @Builder.Default: 빌더로 객체 생성 시, 이 필드에 값이 없으면 아래 초기값을 사용합니다.
     * tokenType: 인증 요청 시 사용될 토큰의 타입 (JWT의 표준은 "Bearer"입니다).
     */
    @Builder.Default
    private String tokenType = "Bearer";
}
