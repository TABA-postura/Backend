package com.postura.auth.handler;

import com.postura.auth.service.JwtTokenProvider;
import com.postura.config.properties.AppProperties;
import com.postura.user.domain.CustomOAuth2User; // 필요시 사용
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User; // 💡 ClassCastException 해결을 위한 필수 임포트
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler; // 💡 오타 수정 완료
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
// 🔥 수정된 부분: SimpleUrlAuthenticationSuccessHandler로 클래스 이름 복원
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final AppProperties appProperties;
    // private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        // 1. 인증된 사용자 정보 획득 (DefaultOidcUser/DefaultOAuth2User 객체를 OAuth2User 인터페이스로 안전하게 받음)
        // 🔥 ClassCastException을 해결하는 핵심 코드
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        // 2. JWT 토큰 생성
        String userId = principal.getName();
        String accessToken = tokenProvider.createAccessToken(userId);
        String refreshToken = tokenProvider.createRefreshToken(userId);

        log.info("OAuth2 인증 성공. 사용자 ID: {}, Access Token 생성 완료", userId);

        // 3. 리프레시 토큰 저장 (Redis 또는 DB)
        // ... (생략)

        // 4. 리다이렉트 URL 생성
        String targetUrl = determineTargetUrl(request, response, authentication);

        String redirectUri = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
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