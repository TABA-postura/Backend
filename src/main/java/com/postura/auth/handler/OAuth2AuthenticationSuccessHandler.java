package com.postura.auth.handler;

import com.postura.auth.domain.RefreshToken;
import com.postura.auth.repository.RefreshTokenRepository;
import com.postura.auth.service.JwtTokenProvider;
import com.postura.config.properties.AppProperties;
import com.postura.dto.auth.TokenResponse;
import com.postura.user.domain.CustomOAuth2User;
import io.jsonwebtoken.Claims;
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
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // 1) (OAuth2 옵션 A) 공용 토큰 발급 로직 사용
        TokenResponse tokenResponse = tokenProvider.generateToken(authentication);

        // 2) RefreshToken upsert를 위한 userId 확보
        Long userId = extractUserId(authentication, tokenResponse);

        // 3) Refresh Token 저장/갱신 (Upsert)
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

        log.info("OAuth2 인증 성공 | userId={} | RefreshToken 저장/갱신 완료", userId);

        // 4) 최종 리다이렉트 URL 생성
        String targetUrl = determineTargetUrl(request, response, authentication);

        String redirectUri = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("accessToken", tokenResponse.getAccessToken())
                .queryParam("refreshToken", tokenResponse.getRefreshToken())
                .build()
                .toUriString();

        // 5) 인증 관련 임시 속성 정리
        clearAuthenticationAttributes(request);

        // 6) 프론트로 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    /**
     * 최종 리다이렉트 URL(프론트엔드 주소) 결정
     */
    @Override
    protected String determineTargetUrl(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        return appProperties.getOauth2().getAuthorizedRedirectUri();
    }

    /**
     * OAuth2 로그인 성공 시 RefreshToken upsert에 필요한 userId를 안정적으로 추출
     * - 1순위: CustomOAuth2User.getName() (CustomOAuth2UserService가 DB userId를 name으로 세팅했다는 전제)
     * - 2순위: AccessToken claims의 userId
     */
    private Long extractUserId(Authentication authentication, TokenResponse tokenResponse) {
        Object principal = authentication.getPrincipal();

        // 1) CustomOAuth2User면 name이 DB userId(String)인 패턴 우선
        if (principal instanceof CustomOAuth2User custom) {
            Long parsed = parseLongOrThrow(custom.getName(), "CustomOAuth2User.getName()");
            return parsed;
        }

        // 2) 일반 OAuth2User도 name에 userId를 넣는 경우가 있어 시도
        if (principal instanceof OAuth2User oAuth2User) {
            String name = oAuth2User.getName();
            Long parsed = tryParseLong(name);
            if (parsed != null) return parsed;
        }

        // 3) fallback: accessToken의 userId 클레임
        Claims claims = tokenProvider.getClaims(tokenResponse.getAccessToken());
        String userIdString = claims.get("userId", String.class);
        return parseLongOrThrow(userIdString, "AccessToken.userId claim");
    }

    private Long parseLongOrThrow(String value, String source) {
        Long parsed = tryParseLong(value);
        if (parsed == null) {
            log.error("userId Long 변환 실패 | source={} | value={}", source, value);
            throw new RuntimeException("OAuth2 로그인 userId 파싱 실패: source=" + source + ", value=" + value);
        }
        return parsed;
    }

    private Long tryParseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
