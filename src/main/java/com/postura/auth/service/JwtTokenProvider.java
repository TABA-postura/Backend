package com.postura.auth.service;

import com.postura.user.service.CustomUserDetails;
import com.postura.dto.auth.TokenResponse;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "auth";

    private final Key key;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-expiration-in-milliseconds}") long accessTokenValidityInMilliseconds,
            @Value("${jwt.refresh-token-expiration-in-milliseconds}") long refreshTokenValidityInMilliseconds) {

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidityInMilliseconds = accessTokenValidityInMilliseconds;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidityInMilliseconds;
    }

    /**
     * AccessToken + RefreshToken 생성
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
                        .role(null)                    // AccessToken에서만 권한 의미 있음
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
