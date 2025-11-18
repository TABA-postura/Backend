// auth/service/AuthService.java
package com.postura.auth.service;

import com.postura.auth.service.JwtTokenProvider;
import com.postura.dto.auth.LoginRequest;
import com.postura.dto.auth.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    // Spring Security 설정을 통해 주입받은 AuthenticationManagerBuilder
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final JwtTokenProvider jwtTokenProvider;
    // (선택 사항) private final RefreshTokenRepository refreshTokenRepository; 

    /**
     * 로그인 로직: 사용자 ID/PW를 검증하고 JWT를 발급합니다.
     * * @param request 로그인 요청 DTO (Email, Password)
     * @return 발급된 Access Token과 Refresh Token
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {

        // 1. 사용자 ID/PW 기반으로 인증 객체(토큰) 생성 (아직 인증되지 않은 상태)
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

        // 2. AuthenticationManager를 통해 인증 시도 및 검증
        // 이 과정에서 CustomUserDetailsService의 loadUserByUsername이 호출되며, 
        // 비밀번호 비교(PasswordEncoder 사용)가 자동으로 수행됩니다.
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증이 성공하면, 인증 정보를 기반으로 Access/Refresh Token 생성
        TokenResponse tokenResponse = jwtTokenProvider.generateToken(authentication);

        // 4. (선택) Refresh Token을 DB나 Redis에 저장/업데이트 (토큰 재발급을 위해 필요)
        // 실제 운영 환경에서는 보안을 위해 Refresh Token을 서버 측에서 관리하는 것이 일반적입니다.

        return tokenResponse;
    }

    /**
     * TODO: 토큰 재발급 (Reissue) 로직 구현 예정
     * Refresh Token을 검증하고, 유효하면 새로운 Access Token과 Refresh Token을 발급합니다.
     */
}