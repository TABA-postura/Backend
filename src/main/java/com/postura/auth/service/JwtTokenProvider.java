package com.postura.auth.service;

import com.postura.config.JwtProperties;
import com.postura.dto.auth.TokenResponse;
import com.postura.user.entity.User;
import com.postura.user.service.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final String USER_ID_KEY = "userId";

    // 시간 오차(Clock Skew) 허용 시간 설정 (5초는 일반적인 허용치)
    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 5;

    private final Key key;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        String secretKey = jwtProperties.getSecret();
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidityInMilliseconds = jwtProperties.getAccessTokenExpirationInMilliseconds();
        this.refreshTokenValidityInMilliseconds = jwtProperties.getRefreshTokenExpirationInMilliseconds();
    }

    /**
     * AccessToken + RefreshToken 생성 (LOCAL + OAuth2 공용)
     *
     * AccessToken:
     * - sub(subject) = email (통일)
     * - auth = ROLE_...
     * - userId = String(Long)
     *
     * RefreshToken:
     * - userId = String(Long)
     * - auth 없음
     */
    public TokenResponse generateToken(Authentication authentication) {
        long now = System.currentTimeMillis();
        Date accessExpiration = new Date(now + accessTokenValidityInMilliseconds);
        Date refreshExpiration = new Date(now + refreshTokenValidityInMilliseconds);

        // 1) userId, email 추출 (LOCAL + OAuth2 모두 지원)
        Long userId = extractUserId(authentication);
        String email = extractEmail(authentication);

        // 2) 권한 문자열
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null && !a.isBlank())
                .collect(Collectors.joining(","));

        // 방어: 권한이 비어있으면 ROLE_USER로 fallback (대부분의 서비스에서 기본권한)
        if (authorities.isBlank()) {
            authorities = "ROLE_USER";
        }

        // 3) Access Token 생성 (email을 subject로 통일)
        String accessToken = Jwts.builder()
                .setSubject(email) // ✅ 통일: email
                .claim(AUTHORITIES_KEY, authorities)
                .claim(USER_ID_KEY, String.valueOf(userId)) // ✅ String 통일
                .setExpiration(accessExpiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // 4) Refresh Token 생성 (auth 없음)
        String refreshToken = Jwts.builder()
                .claim(USER_ID_KEY, String.valueOf(userId)) // ✅ String 통일
                .setExpiration(refreshExpiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * (레거시) OAuth2용 AccessToken 생성 메서드
     * - 현재 권장안에서는 사용하지 않습니다.
     * - OAuth2 성공 시에도 generateToken(authentication)을 사용하도록 SuccessHandler를 수정할 예정입니다.
     */
    @Deprecated
    public String createAccessToken(String userId) {
        throw new UnsupportedOperationException("createAccessToken(userId)는 더 이상 사용하지 않습니다. generateToken(authentication)을 사용하세요.");
    }

    /**
     * (레거시) OAuth2용 RefreshToken 생성 메서드
     * - 현재 권장안에서는 사용하지 않습니다.
     */
    @Deprecated
    public String createRefreshToken(String userId) {
        throw new UnsupportedOperationException("createRefreshToken(userId)는 더 이상 사용하지 않습니다. generateToken(authentication)을 사용하세요.");
    }

    /**
     * Authorization 헤더에서 Bearer 토큰만 추출
     */
    public String resolveToken(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /**
     * JWT → Claims 변환
     */
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * AccessToken에서 Authentication 생성
     */
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        return getAuthenticationFromClaims(claims, token);
    }

    /**
     * Claims 기반 Authentication 생성
     *
     * 보안상 원칙:
     * - Authorization Bearer로 들어오는 토큰은 "AccessToken"이어야 함
     * - 따라서 auth 클레임이 없거나 비어있으면(=RefreshToken) 인증을 만들지 않도록 실패 처리
     */
    public Authentication getAuthenticationFromClaims(Claims claims, String token) {

        // 1) userId 추출 (String -> Long)
        String userIdString = claims.get(USER_ID_KEY, String.class);
        if (userIdString == null || userIdString.isBlank()) {
            throw new JwtException("JWT에 'userId' 클레임이 누락되었습니다.");
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdString);
        } catch (NumberFormatException e) {
            log.error("JWT userId 클레임 변환 오류: String '{}' to Long 실패", userIdString);
            throw new JwtException("Invalid user ID format in token: " + userIdString);
        }

        // 2) 권한(auth) 추출 (없으면 RefreshToken이므로 실패)
        String auth = claims.get(AUTHORITIES_KEY, String.class);
        if (auth == null || auth.isBlank()) {
            throw new JwtException("권한(auth) 클레임이 없습니다. AccessToken이 아닐 가능성이 큽니다.");
        }

        Collection<? extends GrantedAuthority> authorities = Arrays.stream(auth.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 3) principal(UserDetails) 구성
        // subject는 email로 통일되어 있어야 함
        String email = claims.getSubject();
        if (email == null || email.isBlank()) {
            // subject가 없으면 인증 컨텍스트에서 email을 쓰는 기능이 깨질 수 있으므로 방어
            email = "unknown";
        }

        User.Role userRole = User.Role.USER; // principal 생성용 최소값(실제 인가는 authorities로 판단)

        CustomUserDetails principal = new CustomUserDetails(
                User.builder()
                        .id(userId)
                        .email(email)
                        .passwordHash("")  // 필요 없음
                        .name("N/A")       // 필요 없음
                        .role(userRole)
                        .build()
        );

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /**
     * JWT 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
                    .build()
                    .parseClaimsJws(token);
            return true;

        } catch (SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 비어 있습니다: {}", e.getMessage());
        } catch (JwtException e) {
            log.info("JWT 처리 중 오류 발생: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Refresh Token TTL 반환
     */
    public long getRefreshTokenExpirationInMilliseconds() {
        return refreshTokenValidityInMilliseconds;
    }

    // ===========================
    // 내부 헬퍼
    // ===========================

    private Long extractUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        // LOCAL
        if (principal instanceof CustomUserDetails cud) {
            return cud.getUserId();
        }

        // OAuth2 (CustomOAuth2User가 OAuth2User를 구현하는 경우가 대부분)
        if (principal instanceof OAuth2User oAuth2User) {
            // 1) name이 DB userId(String)인 케이스 대응
            Long fromName = parseLongOrNull(oAuth2User.getName());
            if (fromName != null) return fromName;

            // 2) attributes에서 흔히 쓰는 키들 탐색
            Map<String, Object> attrs = oAuth2User.getAttributes();
            for (String key : List.of("userId", "id", "dbId")) {
                Object v = attrs.get(key);
                if (v != null) {
                    Long parsed = parseLongOrNull(String.valueOf(v));
                    if (parsed != null) return parsed;
                }
            }
        }

        throw new RuntimeException("토큰 발급을 위한 userId 추출 실패: principal 타입=" + principal.getClass());
    }

    private String extractEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        // LOCAL
        if (principal instanceof CustomUserDetails cud) {
            return cud.getUsername(); // email
        }

        // 1) CustomOAuth2User에 getEmail()이 있는 케이스(리플렉션)
        String viaGetter = tryInvokeStringGetter(principal, "getEmail");
        if (viaGetter != null && !viaGetter.isBlank()) {
            return viaGetter;
        }

        // 2) OAuth2User attributes에서 email 키 탐색
        if (principal instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            if (email != null && !email.isBlank()) return email;

            // 일부 공급자/매핑에서 다른 키를 쓰는 경우 대비(프로젝트 매핑에 맞게 추후 확장 가능)
            Object v = oAuth2User.getAttributes().get("email");
            if (v != null && !String.valueOf(v).isBlank()) return String.valueOf(v);
        }

        // 3) 마지막 fallback (일관성은 떨어질 수 있음)
        String name = authentication.getName();
        if (name != null && !name.isBlank()) return name;

        throw new RuntimeException("토큰 발급을 위한 email 추출 실패: principal 타입=" + principal.getClass());
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String tryInvokeStringGetter(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            if (v instanceof String s) return s;
        } catch (Exception ignore) {
            // ignore
        }
        return null;
    }
}
