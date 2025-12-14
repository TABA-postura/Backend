package com.postura.auth.service;

import com.postura.auth.domain.RefreshToken;
import com.postura.auth.repository.RefreshTokenRepository;
import com.postura.dto.auth.TokenResponse;
import com.postura.user.entity.User;
import com.postura.user.entity.User.AuthProvider;
import com.postura.user.repository.UserRepository;
import com.postura.user.service.CustomUserDetails;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    /* =========================
       OAuth 설정값
       ========================= */

    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.google.client-secret}")
    private String googleClientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String googleRedirectUri;

    /* =========================
       Public API
       ========================= */

    /**
     * OAuth 로그인 공통 진입점
     */
    @Transactional
    public TokenResponse login(AuthProvider provider, String code) {

        OAuthUserInfo userInfo = switch (provider) {
            case KAKAO -> getKakaoUserInfo(code);
            case GOOGLE -> getGoogleUserInfo(code);
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth Provider");
        };

        // 1️⃣ 사용자 조회 또는 생성
        User user = userRepository
                .findByProviderAndProviderId(
                        userInfo.provider(),
                        userInfo.providerId()
                )
                .orElseGet(() ->
                        userRepository.save(
                                User.createSocialUser(
                                        userInfo.email(),
                                        userInfo.name(),
                                        userInfo.provider(),
                                        userInfo.providerId()
                                )
                        )
                );

        // 2️⃣ CustomUserDetails 생성 (JwtTokenProvider 전제 조건)
        CustomUserDetails principal = new CustomUserDetails(user);

        // 3️⃣ Authentication 생성 (LOCAL 로그인과 동일 구조)
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                );

        // 4️⃣ JWT 발급
        TokenResponse tokenResponse =
                jwtTokenProvider.generateToken(authentication);

        // 5️⃣ RefreshToken Upsert
        Claims claims =
                jwtTokenProvider.getClaims(tokenResponse.getAccessToken());
        Long userId =
                claims.get("userId", Long.class);

        refreshTokenRepository.findById(userId)
                .ifPresentOrElse(
                        token -> token.updateToken(tokenResponse.getRefreshToken()),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .userId(userId)
                                        .token(tokenResponse.getRefreshToken())
                                        .build()
                        )
                );

        log.info("OAuth 로그인 성공 | provider={} userId={}", provider, userId);
        return tokenResponse;
    }

    /* =========================
       Kakao
       ========================= */

    private OAuthUserInfo getKakaoUserInfo(String code) {

        // 1. Access Token 요청
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> tokenRequest = new HttpEntity<>(
                "grant_type=authorization_code" +
                        "&client_id=" + kakaoClientId +
                        "&redirect_uri=" + kakaoRedirectUri +
                        "&code=" + code,
                headers
        );

        ResponseEntity<Map> tokenResponse =
                restTemplate.exchange(
                        "https://kauth.kakao.com/oauth/token",
                        HttpMethod.POST,
                        tokenRequest,
                        Map.class
                );

        String accessToken =
                (String) tokenResponse.getBody().get("access_token");

        // 2. 사용자 정보 요청
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);

        ResponseEntity<Map> userResponse =
                restTemplate.exchange(
                        "https://kapi.kakao.com/v2/user/me",
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders),
                        Map.class
                );

        Map<String, Object> body = userResponse.getBody();
        Map<String, Object> kakaoAccount =
                (Map<String, Object>) body.get("kakao_account");
        Map<String, Object> profile =
                (Map<String, Object>) kakaoAccount.get("profile");

        return new OAuthUserInfo(
                AuthProvider.KAKAO,
                String.valueOf(body.get("id")),
                (String) kakaoAccount.get("email"),
                (String) profile.get("nickname")
        );
    }

    /* =========================
       Google
       ========================= */

    private OAuthUserInfo getGoogleUserInfo(String code) {

        // 1. Access Token 요청
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> tokenRequest = new HttpEntity<>(
                "grant_type=authorization_code" +
                        "&client_id=" + googleClientId +
                        "&client_secret=" + googleClientSecret +
                        "&redirect_uri=" + googleRedirectUri +
                        "&code=" + code,
                headers
        );

        ResponseEntity<Map> tokenResponse =
                restTemplate.exchange(
                        "https://oauth2.googleapis.com/token",
                        HttpMethod.POST,
                        tokenRequest,
                        Map.class
                );

        String accessToken =
                (String) tokenResponse.getBody().get("access_token");

        // 2. 사용자 정보 요청
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);

        ResponseEntity<Map> userResponse =
                restTemplate.exchange(
                        "https://www.googleapis.com/oauth2/v2/userinfo",
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders),
                        Map.class
                );

        Map<String, Object> body = userResponse.getBody();

        return new OAuthUserInfo(
                AuthProvider.GOOGLE,
                (String) body.get("id"),
                (String) body.get("email"),
                (String) body.get("name")
        );
    }

    /* =========================
       내부 DTO
       ========================= */

    private record OAuthUserInfo(
            AuthProvider provider,
            String providerId,
            String email,
            String name
    ) {}
}
