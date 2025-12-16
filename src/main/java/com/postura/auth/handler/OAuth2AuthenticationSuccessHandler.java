package com.postura.auth.handler;

import com.postura.auth.domain.RefreshToken; // RefreshToken 엔티티 임포트
import com.postura.auth.repository.RefreshTokenRepository; // RefreshTokenRepository 임포트
import com.postura.auth.service.JwtTokenProvider;
import com.postura.config.properties.AppProperties;
import com.postura.dto.auth.TokenResponse; // TokenResponse DTO 임포트 (선택 사항이지만 유용)
import com.postura.user.domain.CustomOAuth2User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 처리를 위해 임포트
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final AppProperties appProperties;
    private final RefreshTokenRepository refreshTokenRepository; // ⭐ 추가: RefreshToken 저장소 주입

    @Transactional // ⭐ 추가: DB 저장 로직이 포함되므로 트랜잭션 필요
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        // 1. 인증된 사용자 정보 획득 및 ID 추출
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        // CustomOAuth2User를 사용했다면 getName()이 userId(String)를 반환하도록 설정했을 가능성이 높음
        String userIdString = principal.getName();
        Long userId;
        try {
            userId = Long.valueOf(userIdString);
        } catch (NumberFormatException e) {
            log.error("OAuth2 사용자 ID 클레임 변환 오류: {}", userIdString);
            throw new RuntimeException("OAuth2 인증 성공 후 사용자 ID 형식이 유효하지 않습니다: " + userIdString);
        }

        // 2. JWT 토큰 생성 (AccessToken, RefreshToken 모두 생성)
        TokenResponse tokenResponse = tokenProvider.generateToken(authentication);

        log.info("OAuth2 인증 성공. 사용자 ID: {}, Access Token 생성 완료", userId);

        // 3. ⭐ Refresh Token 저장/갱신 (Upsert) - AuthService.login 로직 재사용
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
        log.info("Refresh Token 저장/갱신 완료 | userId={}", userId);


        // 4. 리다이렉트 URL 생성
        String targetUrl = determineTargetUrl(request, response, authentication);

        String redirectUri = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("accessToken", tokenResponse.getAccessToken())
                .queryParam("refreshToken", tokenResponse.getRefreshToken())
                .build().toUriString();

        // 5. 프론트엔드 URL로 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    /**
     * 최종 리다이렉트 URL (프론트엔드 주소)을 결정합니다.
     */
    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // AppProperties에서 설정된 최종 프론트엔드 URI를 반환합니다.
        return appProperties.getOauth2().getAuthorizedRedirectUri();
    }
}