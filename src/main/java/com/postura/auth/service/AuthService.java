package com.postura.auth.service;

import com.postura.auth.domain.RefreshToken;
import com.postura.auth.repository.RefreshTokenRepository;
import com.postura.dto.auth.LoginRequest;
import com.postura.dto.auth.TokenResponse;
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

    // RefreshToken을 RDB로 관리
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 로그인 로직: 사용자 인증 후 Access/Refresh Token 발급 및 Refresh Token 저장/갱신 (Upsert).
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

        // 1. 인증 수행
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 2. JWT 토큰 생성
        TokenResponse tokenResponse = jwtTokenProvider.generateToken(authentication);

        // 3. AccessToken에서 userId 추출
        Claims claims = jwtTokenProvider.getClaims(tokenResponse.getAccessToken());
        Long userId = claims.get("userId", Long.class);

        // 4. Refresh Token 저장/갱신 (Upsert 로직 적용)
        // userId가 RefreshToken 엔티티의 ID이므로 findById 사용으로 변경
        refreshTokenRepository.findById(userId)
                .ifPresentOrElse(
                        // 존재하면: 토큰 값만 갱신
                        existingToken -> existingToken.updateToken(tokenResponse.getRefreshToken()),
                        // 없으면: 새로 생성하여 저장
                        () -> {
                            RefreshToken newRefreshToken = RefreshToken.builder()
                                    .userId(userId) // userId를 먼저 설정 (ID)
                                    .token(tokenResponse.getRefreshToken())
                                    .build();
                            refreshTokenRepository.save(newRefreshToken);
                        }
                );

        log.info("로그인 성공 — Refresh Token 저장/갱신 완료 | userId={}", userId);

        return tokenResponse;
    }

    /**
     * Refresh Token으로 Access Token 재발급(reissue) 및 Refresh Token 갱신.
     */
    @Transactional
    public TokenResponse reissue(String requestRefreshToken) {

        // 1. Refresh Token 검증
        if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh Token입니다.");
        }

        // 2. DB에 저장된 Refresh Token인지 확인
        RefreshToken storedRefreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("서버에 존재하지 않는 Refresh Token입니다."));

        // 3. 토큰 Claims 파싱
        Claims claims = jwtTokenProvider.getClaims(requestRefreshToken);

        // 4. Authentication 객체 생성
        Authentication authentication = jwtTokenProvider.getAuthenticationFromClaims(claims, requestRefreshToken);

        // 5. 새로운 Access/Refresh Token 생성
        TokenResponse newTokenResponse = jwtTokenProvider.generateToken(authentication);

        // 6. 기존 Refresh Token을 새 토큰 값으로 갱신 (Update)
        // RefreshToken 엔티티의 ID(userId)는 불변하며, token 필드만 변경됩니다.
        storedRefreshToken.updateToken(newTokenResponse.getRefreshToken());

        log.info("토큰 재발급 완료 — userId={}", storedRefreshToken.getUserId());
        return newTokenResponse;
    }

    /**
     * 로그아웃 — AccessToken을 기반으로 유저 식별 후 RefreshToken 제거
     */
    @Transactional
    public void logout(String authorizationHeader) {

        // 1. Bearer 토큰 파싱
        String token = jwtTokenProvider.resolveToken(authorizationHeader);
        if (token == null) {
            throw new RuntimeException("Authorization 헤더가 유효하지 않습니다.");
        }

        Claims claims;
        try {
            // 2. Access Token 유효성 체크 및 Claims 추출
            claims = jwtTokenProvider.getClaims(token);
        } catch (ExpiredJwtException e) {
            // Access Token이 만료된 경우 (로그아웃은 가능해야 함)
            claims = e.getClaims();
            log.warn("만료된 Access Token으로 로그아웃 시도 | message: {}", e.getMessage());
        } catch (Exception e) {
            // 서명 위변조 등 다른 심각한 문제인 경우
            log.error("유효하지 않은 Access Token입니다.", e);
            throw new RuntimeException("유효하지 않거나 손상된 Access Token입니다.");
        }

        // 3. userId 추출
        Long userId = claims.get("userId", Long.class);

        // 4. DB에 저장된 Refresh Token 제거
        // userId는 RefreshToken 엔티티의 ID이므로, deleteById를 사용
        refreshTokenRepository.deleteById(userId);

        log.info("로그아웃 성공 — Refresh Token 삭제 | userId={}", userId);
    }
}