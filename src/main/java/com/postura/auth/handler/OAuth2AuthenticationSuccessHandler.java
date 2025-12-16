package com.postura.auth.handler;

import com.postura.auth.domain.RefreshToken;
import com.postura.auth.repository.RefreshTokenRepository;
import com.postura.auth.service.JwtTokenProvider;
import com.postura.config.properties.AppProperties;
import com.postura.dto.auth.TokenResponse;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final AppProperties appProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        // 1. 인증된 사용자 정보 획득 및 ID 추출
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        // CustomOAuth2UserService에서 user.getId().toString()을 Principal Name으로 설정했다고 가정합니다.
        String userIdString = principal.getName();

        Long userId;
        try {
            // ⭐ 복구된 Long 변환 로직: 이제 DB의 Long ID 문자열이 넘어오므로 성공해야 합니다.
            userId = Long.valueOf(userIdString);
        } catch (NumberFormatException e) {
            // 이 예외는 CustomOAuth2UserService에서 아직도 Google의 긴 ID를 반환하고 있음을 의미합니다.
            log.error("DB Long ID 변환 오류. Principal Name: {}", userIdString);
            throw new RuntimeException("DB Long ID 변환에 실패했습니다. CustomOAuth2UserService의 반환 값을 확인하십시오.", e);
        }

        // 2. JWT 토큰 생성 (AccessToken, RefreshToken 모두 생성)
        TokenResponse tokenResponse = tokenProvider.generateToken(authentication);

        log.info("OAuth2 인증 성공. DB User ID: {}, Access Token 생성 완료", userId);

        // 3. ⭐ Refresh Token 저장/갱신 (Upsert) - Long userId 사용
        refreshTokenRepository.findById(userId)
                .ifPresentOrElse(
                        existing -> existing.updateToken(tokenResponse.getRefreshToken()),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .userId(userId) // Long ID 사용
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
        return appProperties.getOauth2().getAuthorizedRedirectUri();
    }
}