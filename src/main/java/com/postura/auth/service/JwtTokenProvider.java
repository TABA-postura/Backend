package com.postura.auth.service;

import com.postura.user.entity.User;
import com.postura.user.service.CustomUserDetails;
import com.postura.dto.auth.TokenResponse;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

// Configuration Properties 클래스 임포트
import com.postura.config.JwtProperties;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    // 시간 오차(Clock Skew) 허용 시간 설정 (5초는 일반적인 허용치입니다.)
    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 5;

    private final Key key;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    // 🔥 JwtProperties 주입 생성자 (PlaceholderResolutionException 해결)
    public JwtTokenProvider(JwtProperties jwtProperties) {

        String secretKey = jwtProperties.getSecret();
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

        this.key = Keys.hmacShaKeyFor(keyBytes);

        this.accessTokenValidityInMilliseconds = jwtProperties.getAccessTokenExpirationInMilliseconds();
        this.refreshTokenValidityInMilliseconds = jwtProperties.getRefreshTokenExpirationInMilliseconds();
    }

    /**
     * AccessToken + RefreshToken 생성 (일반 로그인용)
     */
    public TokenResponse generateToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = System.currentTimeMillis();
        Date accessExpiration = new Date(now + accessTokenValidityInMilliseconds);
        Date refreshExpiration = new Date(now + refreshTokenValidityInMilliseconds);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();

        // Access Token 생성
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())       // email
                .claim(AUTHORITIES_KEY, authorities)
                .claim("userId", userId)
                .setExpiration(accessExpiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Refresh Token 생성 (auth 없음)
        String refreshToken = Jwts.builder()
                .claim("userId", userId)
                .setExpiration(refreshExpiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // =========================================================================
    // OAuth2AuthenticationSuccessHandler에서 사용할 메서드 추가
    // =========================================================================

    /**
     * Access Token을 생성합니다. (OAuth2용)
     */
    public String createAccessToken(String userId) {
        long now = System.currentTimeMillis();
        Date accessExpiration = new Date(now + accessTokenValidityInMilliseconds);

        // Access Token 생성
        return Jwts.builder()
                .setSubject(userId)
                .claim("userId", userId) // userId는 String 타입으로 저장됨
                .setExpiration(accessExpiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Refresh Token을 생성합니다. (OAuth2용)
     */
    public String createRefreshToken(String userId) {
        long now = System.currentTimeMillis();
        Date refreshExpiration = new Date(now + refreshTokenValidityInMilliseconds);

        // Refresh Token 생성 (userId 클레임만 사용)
        return Jwts.builder()
                .claim("userId", userId)
                .setExpiration(refreshExpiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // =========================================================================

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
                // 🔥 Clock Skew 허용 설정 추가 (ExpiredJwtException 해결)
                .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * AccessToken 또는 RefreshToken에서 Authentication 생성
     */
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        return getAuthenticationFromClaims(claims, token);
    }

    /**
     * Claims 기반 Authentication 생성
     * (RefreshToken에는 auth가 없기 때문에 null 대비 처리 포함)
     */
    public Authentication getAuthenticationFromClaims(Claims claims, String token) {

        // 🚨 수정: OAuth2 토큰 생성 시 String으로 저장된 userId 클레임을 String으로 읽고, Long으로 변환
        String userIdString = claims.get("userId", String.class);
        Long userId = null;

        if (userIdString != null) {
            try {
                // String을 Long으로 변환 (DB ID 타입에 맞춤)
                userId = Long.valueOf(userIdString);
            } catch (NumberFormatException e) {
                log.error("JWT userId 클레임 변환 오류: String '{}' to Long 실패", userIdString);
                // 변환 실패 시 예외를 던지거나, 인증 실패로 처리 (여기서는 런타임 예외로 처리)
                throw new JwtException("Invalid user ID format in token: " + userIdString);
            }
        }

        // 권한이 있을 수도 있고 없을 수도 있음
        Collection<? extends GrantedAuthority> authorities = new ArrayList<>();
        User.Role userRole = User.Role.USER;

        if (claims.get(AUTHORITIES_KEY) != null) {
            authorities = Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        // UserDetails 생성
        CustomUserDetails principal = new CustomUserDetails(
                com.postura.user.entity.User.builder()
                        .id(userId)
                        .email(claims.getSubject())    // AccessToken일 때만 접근 가능
                        .passwordHash("")              // 필요 없음
                        .name("N/A")                   // 필요 없음
                        .role(userRole)                    // AccessToken에서만 권한 의미 있음
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
                    // 🔥 Clock Skew 허용 설정 추가 (ExpiredJwtException 해결)
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
            log.info("JWT 처리 중 오류 발생: {}", e.getMessage()); // 추가된 RuntimeException 처리
        }
        return false;
    }

    /**
     * Refresh Token TTL 반환
     */
    public long getRefreshTokenExpirationInMilliseconds() {
        return refreshTokenValidityInMilliseconds;
    }
}