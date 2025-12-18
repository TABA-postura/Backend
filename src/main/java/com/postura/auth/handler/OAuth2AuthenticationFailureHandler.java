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

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AppProperties appProperties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        log.error("OAuth2 로그인 실패: {}", exception.getMessage(), exception);

        String targetUrl = appProperties.getOauth2().getAuthorizedRedirectUri();

        // 기본 에러 코드
        String errorCode = "oauth2_login_failed";

        // provider_mismatch 등 OAuth2Error 코드 추출
        if (exception instanceof OAuth2AuthenticationException oauth2Ex
                && oauth2Ex.getError() != null
                && oauth2Ex.getError().getErrorCode() != null) {
            errorCode = oauth2Ex.getError().getErrorCode();
        }

        // 메시지 (여기서 URLEncoder 금지: UriComponentsBuilder가 인코딩 처리)
        String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
                ? exception.getMessage()
                : "소셜 로그인에 실패했습니다.";

        // 어떤 provider로 시도하다 실패했는지(google/kakao) — 프론트 분기용
        String provider = extractProviderFromRequest(request);

        String redirectUri = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", errorCode)
                .queryParam("message", message)
                .queryParam("provider", provider)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    /**
     * /oauth2/authorization/{provider} 또는 /login/oauth2/code/{provider} 등에서 provider 추출
     */
    private String extractProviderFromRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) return "unknown";

        // 예: /oauth2/authorization/google
        if (uri.startsWith("/oauth2/authorization/")) {
            return uri.substring("/oauth2/authorization/".length());
        }

        // 예: /login/oauth2/code/google
        if (uri.startsWith("/login/oauth2/code/")) {
            return uri.substring("/login/oauth2/code/".length());
        }

        return "unknown";
    }
}
