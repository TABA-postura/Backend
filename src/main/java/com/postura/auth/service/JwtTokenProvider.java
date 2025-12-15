package com.postura.auth.service;

import com.postura.user.entity.User;
import com.postura.user.service.CustomUserDetails;
import com.postura.dto.auth.TokenResponse;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value; // 🔥 @Value 제거
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

// 🔥 Configuration Properties 클래스 임포트 (이 클래스가 별도 파일로 존재해야 합니다.)
import com.postura.config.JwtProperties;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "auth";

    private final Key key;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    // 🔥 기존 @Value 생성자를 삭제하고, JwtProperties를 주입받는 생성자로 교체
    public JwtTokenProvider(JwtProperties jwtProperties) {

        // 1. Secret Key 처리: Properties 객체에서 값을 가져옴
        String secretKey = jwtProperties.getSecret();

        // 2. 키 초기화 로직은 그대로 유지
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

        this.key = Keys.hmacShaKeyFor(keyBytes);

        // 3. 만료 시간 설정: Properties 객체에서 값을 가져와 필드에 할당
        this.accessTokenValidityInMilliseconds = jwtProperties.getAccessTokenExpirationInMilliseconds();
        this.refreshTokenValidityInMilliseconds = jwtProperties.getRefreshTokenExpirationInMilliseconds();
    }

    /**
     * AccessToken + RefreshToken 생성 (일반 로그인용)
     */
    public TokenResponse generateToken(Authentication authentication) {
        // ... (기존 로직 유지)
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
     * @param userId 토큰의 주체(Subject)로 사용할 사용자 ID (String 형태)
     * @return 생성된 JWT Access Token
     */
    public String createAccessToken(String userId) {
        long now = System.currentTimeMillis();
        Date accessExpiration = new Date(now + accessTokenValidityInMilliseconds);

        // Access Token 생성 (권한 정보 및 email(Subject)은 임시로 userId로 대체)
        return Jwts.builder()
                .setSubject(userId)
                .claim("userId", userId)
                // TODO: OAuth2 성공 후 권한을 찾아서 claim(AUTHORITIES_KEY, authorities) 추가 필요
                .setExpiration(accessExpiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Refresh Token을 생성합니다. (OAuth2용)
     * @param userId 토큰의 주체(Subject)로 사용할 사용자 ID (String 형태)
     * @return 생성된 JWT Refresh Token
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

        Long userId = claims.get("userId", Long.class);

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
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;

        } catch (SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명: {}", e.getMessage());

        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰: {}", e.getMessage());

        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰: {}", e.getMessage());

        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 비어 있습니다: {}", e.getMessage());
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