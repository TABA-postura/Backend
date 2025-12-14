package com.postura.auth.service;

import com.postura.auth.domain.RefreshToken;
import com.postura.auth.repository.RefreshTokenRepository;
import com.postura.dto.auth.LoginRequest;
import com.postura.dto.auth.TokenResponse;
import com.postura.user.entity.User;
import com.postura.user.entity.User.AuthProvider;
import com.postura.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * LOCAL 로그인 전용
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {

        // 0. 사용자 존재 및 로그인 타입 검증
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new RuntimeException("소셜 로그인 계정입니다. 소셜 로그인을 이용하세요.");
        }

        if (user.getPasswordHash() == null) {
            throw new RuntimeException("비밀번호가 설정되지 않은 계정입니다.");
        }

        // 1. Spring Security 인증 처리
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                );

        Authentication authentication =
                authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 2. JWT 토큰 생성
        TokenResponse tokenResponse =
                jwtTokenProvider.generateToken(authentication);

        // 3. AccessToken에서 userId 추출
        Claims claims =
                jwtTokenProvider.getClaims(tokenResponse.getAccessToken());
        Long userId =
                claims.get("userId", Long.class);

        // 4. Refresh Token 저장/갱신 (Upsert)
        refreshTokenRepository.findById(userId)
                .ifPresentOrElse(
                        existing -> existing.updateToken(tokenResponse.getRefreshToken()),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .userId(userId)
                                        .token(tokenResponse.getRefreshToken())
                                        .build()
                        )
                );

        log.info("LOCAL 로그인 성공 | userId={}", userId);
        return tokenResponse;
    }

    /**
     * Refresh Token 재발급
     */
    @Transactional
    public TokenResponse reissue(String requestRefreshToken) {

        if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh Token입니다.");
        }

        RefreshToken storedRefreshToken =
                refreshTokenRepository.findByToken(requestRefreshToken)
                        .orElseThrow(() -> new RuntimeException("서버에 존재하지 않는 Refresh Token입니다."));

        Claims claims =
                jwtTokenProvider.getClaims(requestRefreshToken);

        Authentication authentication =
                jwtTokenProvider.getAuthenticationFromClaims(claims, requestRefreshToken);

        TokenResponse newTokenResponse =
                jwtTokenProvider.generateToken(authentication);

        storedRefreshToken.updateToken(newTokenResponse.getRefreshToken());

        log.info("토큰 재발급 완료 | userId={}", storedRefreshToken.getUserId());
        return newTokenResponse;
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(String authorizationHeader) {

        String token =
                jwtTokenProvider.resolveToken(authorizationHeader);
        if (token == null) {
            throw new RuntimeException("Authorization 헤더가 유효하지 않습니다.");
        }

        Claims claims;
        try {
            claims = jwtTokenProvider.getClaims(token);
        } catch (ExpiredJwtException e) {
            claims = e.getClaims();
            log.warn("만료된 Access Token으로 로그아웃 시도");
        }

        Long userId =
                claims.get("userId", Long.class);

        refreshTokenRepository.deleteById(userId);

        log.info("로그아웃 완료 | userId={}", userId);
    }
}
