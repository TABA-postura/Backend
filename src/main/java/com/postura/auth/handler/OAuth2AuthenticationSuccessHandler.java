package com.postura.auth.handler;

import com.postura.auth.service.JwtTokenProvider;
import com.postura.config.properties.AppProperties;
import com.postura.user.domain.CustomOAuth2User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final AppProperties appProperties;

    // ⚠️ 주의: CustomOAuth2User는 실제 프로젝트의 OAuth2 User 클래스 이름으로 대체해야 합니다.
    // ⚠️ 주의: AppProperties는 app.oauth2.authorized-redirect-uri 설정을 읽어오기 위해 필요합니다.

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        // 1. 인증된 사용자 정보 (CustomOAuth2User 객체) 획득
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        // 2. JWT 토큰 생성
        String accessToken = tokenProvider.createAccessToken(oAuth2User.getName());
        String refreshToken = tokenProvider.createRefreshToken(oAuth2User.getName());

        log.info("OAuth2 인증 성공. 사용자: {}, Access Token 생성 완료", oAuth2User.getName());

        // 3. 리프레시 토큰 저장 (Redis 또는 DB)
        // TODO: Redis나 DB에 refreshToken을 저장하는 서비스 로직을 호출해야 합니다.
        // 예시: refreshTokenService.saveRefreshToken(oAuth2User.getName(), refreshToken);

        // 4. 리다이렉트 URL 생성: 프론트엔드로 JWT 토큰을 전달
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
        // application.properties의 app.oauth2.authorized-redirect-uri 값을 사용합니다.
        // 예시: https://taba-postura.com/oauth/redirect
        return appProperties.getOauth2().getAuthorizedRedirectUri();
    }
}