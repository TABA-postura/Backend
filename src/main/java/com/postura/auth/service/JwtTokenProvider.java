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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j // 로깅 사용
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private final Key key;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    // 설정 파일(.yml)에서 JWT 관련 속성들을 주입받습니다.
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-expiration-in-milliseconds}") long accessTokenValidityInMilliseconds,
            @Value("${jwt.refresh-token-expiration-in-milliseconds}") long refreshTokenValidityInMilliseconds) {

        // 1. 비밀 키를 Base64 디코딩하여 Key 객체로 변환합니다.
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidityInMilliseconds = accessTokenValidityInMilliseconds;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidityInMilliseconds;
    }

    /**
     * Authentication 객체를 기반으로 Access Token과 Refresh Token을 생성합니다.
     * @param authentication 인증된 사용자 정보 (Authentication 객체)
     * @return TokenResponse (Access Token 및 Refresh Token 포함)
     */
    public TokenResponse generateToken(Authentication authentication) {

        // 1. 권한 정보 추출
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // 2. 만료 시간 설정
        long now = (new Date()).getTime();
        Date accessExpiration = new Date(now + accessTokenValidityInMilliseconds);
        Date refreshExpiration = new Date(now + refreshTokenValidityInMilliseconds);

        // ** 추가: CustomUserDetails에서 userId 추출
        // CustomUserDetailsService를 통해 load된 Principal은 CustomUserDetails입니다.
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();

        // 3. Access Token 생성 (사용자 ID와 권한 포함)
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName()) // 토큰의 주체 (여기서는 email/Username)
                .claim(AUTHORITIES_KEY, authorities)  // 권한 정보 (Claims)
                .setExpiration(accessExpiration)     // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 서명 (비밀 키로 암호화)
                .compact();

        // 4. Refresh Token 생성 (일반적으로는 Claim 없이 만료 시간만 설정)
        String refreshToken = Jwts.builder()
                .setExpiration(refreshExpiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // 5. TokenResponse 객체로 묶어 반환
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * JWT에서 인증 정보를 추출하여 Spring Security Authentication 객체를 반환합니다.
     * @param token Access Token
     * @return Authentication 객체
     */
    public Authentication getAuthentication(String token) {
        // 1. 토큰 파싱 및 클레임 추출
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // ** 추가: 클레임에서 userId 추출
        Long userIdFromToken = claims.get("userId", Long.class);

        // 2. 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // 3. UserDetails 객체 생성 (비밀번호는 필요 없으므로 null 또는 빈 문자열)
        // 여기서는 토큰의 subject(email)와 권한만으로 Authentication 객체를 생성합니다.
        CustomUserDetails principal = new CustomUserDetails(
                // 임시 User 객체 생성 (실제 DB 조회 아님)
                com.postura.user.entity.User.builder()
                        .id(userIdFromToken) //
                        .email(claims.getSubject())
                        .passwordHash("") // 비밀번호는 토큰에 없으므로 빈 값
                        .name("N/A")
                        .role(com.postura.user.entity.User.Role.valueOf(
                                authorities.iterator().next().getAuthority()
                                        .replace("ROLE_", "")))
                        .build()
        );

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /**
     * 토큰의 유효성을 검증합니다.
     * @param token 검증할 JWT
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }
}