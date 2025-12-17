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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

        String errorCode = "oauth2_login_failed";
        if (exception instanceof OAuth2AuthenticationException oauth2Ex
                && oauth2Ex.getError() != null
                && oauth2Ex.getError().getErrorCode() != null) {
            errorCode = oauth2Ex.getError().getErrorCode(); // ✅ provider_mismatch 등
        }

        String msg = URLEncoder.encode(
                exception.getMessage() != null ? exception.getMessage() : "소셜 로그인에 실패했습니다.",
                StandardCharsets.UTF_8
        );

        String redirectUri = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", errorCode)
                .queryParam("message", msg)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }
}
