package com.postura.dto.auth;

import com.postura.user.entity.User.AuthProvider;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * OAuthService에서 사용자 정보(DB 조회/저장, JWT 발급)를 전달하기 위한 DTO
 */
@Getter
@Setter
@Builder
public class UserInfo {

    private Long userId; // DB ID (조회 후 설정)
    private String email;
    private String name;
    private String picture; // 프로필 사진 URL
    private AuthProvider provider; // 소셜 제공자 (KAKAO, GOOGLE)
    private String providerId; // 소셜 제공자 고유 ID

    // Getter/Setter 대신 Lombok @Getter/@Setter를 사용합니다.

    // 이 DTO는 OAuthService.login(AuthProvider provider, UserInfo userInfo) 메서드의 파라미터로 사용됩니다.
}