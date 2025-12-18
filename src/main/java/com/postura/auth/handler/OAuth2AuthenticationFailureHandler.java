package com.postura.auth.handler;

import com.postura.config.properties.AppProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AppProperties appProperties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        log.error("OAuth2 로그인 실패: {}", exception.getMessage(), exception);

        String targetUrl = appProperties.getOauth2().getAuthorizedRedirectUri();

        // 1) 에러 코드 추출 (provider_mismatch 등)
        String errorCode = "oauth2_login_failed";
        if (exception instanceof OAuth2AuthenticationException oauth2Ex
                && oauth2Ex.getError() != null
                && oauth2Ex.getError().getErrorCode() != null) {
            errorCode = oauth2Ex.getError().getErrorCode();
        }

        // 2) 메시지(한글 포함 가능) - 여기서 URLEncoder로 수동 인코딩하지 말고,
        //    UriComponentsBuilder.encode(...)에 맡기는 것이 안전합니다.
        String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
                ? exception.getMessage()
                : "소셜 로그인에 실패했습니다.";

        // 3) provider(google/kakao) 추출 (콜백 URI: /login/oauth2/code/{registrationId})
        String provider = extractProviderFromRequest(request);

        // 4) ✅ encode(StandardCharsets.UTF_8)로 Location 헤더에 안전한 ASCII(퍼센트 인코딩)만 들어가게 보장
        String redirectUri = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", errorCode)
                .queryParam("message", message)
                .queryParam("provider", provider)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private String extractProviderFromRequest(HttpServletRequest request) {
        // 보통 실패는 /login/oauth2/code/google 또는 /login/oauth2/code/kakao 에서 발생
        String uri = request.getRequestURI();
        if (uri == null) return "";

        // 예: /login/oauth2/code/google
        int idx = uri.lastIndexOf('/');
        if (idx < 0 || idx == uri.length() - 1) return "";
        return uri.substring(idx + 1);
    }
}
