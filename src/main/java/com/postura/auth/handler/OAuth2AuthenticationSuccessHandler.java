package com.postura.auth.handler;

import com.postura.auth.service.OAuthService;
import com.postura.dto.auth.TokenResponse;
import com.postura.user.entity.User.AuthProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthService oAuthService;

    // 🚨 application.properties에서 주입받을 클라이언트 리다이렉트 URI
    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 1. 요청에서 provider (kakao, google) 와 인가 코드(code) 추출
        String code = request.getParameter("code");

        // requestURI: /login/oauth2/code/kakao 또는 /login/oauth2/code/google
        String requestURI = request.getRequestURI();

        AuthProvider provider = extractProviderFromUri(requestURI);

        try {
            // 2. 고객님의 OAuthService를 호출하여 JWT 토큰 발급 및 DB 처리
            TokenResponse tokenResponse = oAuthService.login(provider, code);

            // 3. JWT 토큰을 쿼리 파라미터에 담아 클라이언트로 리다이렉트
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("accessToken", tokenResponse.getAccessToken())
                    .queryParam("refreshToken", tokenResponse.getRefreshToken())
                    .build().toUriString();

            response.sendRedirect(targetUrl);

        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생: {}", e.getMessage());
            // 로그인 실패 시 에러 페이지로 리다이렉트
            String failureUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", "oauth_login_failed")
                    .build().toUriString();

            response.sendRedirect(failureUrl);
        }
    }

    // URI에서 provider 이름을 추출하는 헬퍼 메서드
    private AuthProvider extractProviderFromUri(String uri) {
        if (uri.contains("kakao")) {
            return AuthProvider.KAKAO;
        } else if (uri.contains("google")) {
            return AuthProvider.GOOGLE;
        }
        throw new IllegalArgumentException("지원하지 않는 OAuth Provider URI: " + uri);
    }
}