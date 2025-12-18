package com.postura.auth.service;

import com.postura.auth.domain.RefreshToken;
import com.postura.auth.repository.RefreshTokenRepository;
import com.postura.dto.auth.LoginRequest;
import com.postura.dto.auth.SignUpRequest;
import com.postura.dto.auth.TokenResponse;
import com.postura.user.entity.User;
import com.postura.user.entity.User.AuthProvider;
import com.postura.user.repository.UserRepository;
import com.postura.user.service.CustomUserDetails;
import com.postura.user.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    // ✅ 회원가입은 UserService에 위임 (중복 이메일 처리/비밀번호 인코딩 로직 재사용)
    private final UserService userService;

    /**
     * LOCAL 회원가입 전용
     */
    @Transactional
    public void signUp(SignUpRequest request) {
        userService.signUp(request);
    }

    /**
     * LOCAL 로그인 전용
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {

        // 0) 사용자 존재 및 로그인 타입 검증
        User user = userRepository.findByEmail(request.getEmail())
                // 계정 존재 여부를 외부에 노출하지 않기 위해 401로 통일 권장
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (user.getProvider() != AuthProvider.LOCAL) {
            // 정책상 로컬 로그인 불가 (소셜 계정)
            throw new IllegalStateException("소셜 로그인 계정입니다. 소셜 로그인을 이용하세요.");
        }

        if (user.getPasswordHash() == null) {
            throw new IllegalStateException("비밀번호가 설정되지 않은 계정입니다.");
        }

        // 1) Spring Security 인증 처리 (비밀번호 틀리면 BadCredentialsException 발생)
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

        Authentication authentication =
                authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 2) JWT 토큰 생성
        TokenResponse tokenResponse = jwtTokenProvider.generateToken(authentication);

        // 3) userId 추출: principal 우선 + fallback(토큰 클레임)
        Long userId = extractUserId(authentication, tokenResponse);

        // 4) Refresh Token 저장/갱신 (Upsert)
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

        log.info("LOCAL 로그인 성공 | userId={}", userId);
        return tokenResponse;
    }

    /**
     * Refresh Token 재발급 (옵션 A: RefreshToken -> userId -> DB조회 -> 권한 포함 Authentication 구성)
     */
    @Transactional
    public TokenResponse reissue(String requestRefreshToken) {

        if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
            throw new BadCredentialsException("유효하지 않은 Refresh Token입니다.");
        }

        // DB에 저장된 RefreshToken인지 확인 (서버가 인정한 토큰만 재발급)
        RefreshToken storedRefreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new BadCredentialsException("서버에 존재하지 않는 Refresh Token입니다."));

        // RefreshToken에서 userId 추출
        Claims claims = jwtTokenProvider.getClaims(requestRefreshToken);

        String userIdString = claims.get("userId", String.class);
        if (userIdString == null || userIdString.isBlank()) {
            throw new BadCredentialsException("RefreshToken에 'userId' 클레임이 누락되었습니다.");
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdString);
        } catch (NumberFormatException e) {
            log.error("RefreshToken userId 변환 오류: {}", userIdString);
            throw new BadCredentialsException("RefreshToken 사용자 ID 형식이 유효하지 않습니다.");
        }

        // (방어) DB userId와 클레임 userId 일치 확인
        if (!storedRefreshToken.getUserId().equals(userId)) {
            throw new BadCredentialsException("RefreshToken 정보가 일치하지 않습니다.");
        }

        // DB에서 사용자 조회 (권한 포함)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("사용자 정보를 확인할 수 없습니다."));

        // 권한 포함 Authentication 구성
        CustomUserDetails principal = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        // 새 토큰 발급
        TokenResponse newTokenResponse = jwtTokenProvider.generateToken(authentication);

        // RefreshToken 로테이션(갱신)
        storedRefreshToken.updateToken(newTokenResponse.getRefreshToken());

        log.info("토큰 재발급 완료 | userId={}", userId);
        return newTokenResponse;
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(String authorizationHeader) {

        String token = jwtTokenProvider.resolveToken(authorizationHeader);
        if (token == null) {
            throw new IllegalArgumentException("Authorization 헤더가 유효하지 않습니다.");
        }

        Claims claims;
        try {
            claims = jwtTokenProvider.getClaims(token);
        } catch (ExpiredJwtException e) {
            // 만료된 AccessToken이라도 userId가 있으면 서버 RefreshToken은 제거 가능
            claims = e.getClaims();
            log.warn("만료된 Access Token으로 로그아웃 시도");
        }

        String userIdString = claims.get("userId", String.class);
        if (userIdString == null || userIdString.isBlank()) {
            log.warn("로그아웃 토큰에 'userId' 클레임이 없어 RefreshToken 삭제 생략");
            return;
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdString);
        } catch (NumberFormatException e) {
            log.error("로그아웃 토큰 userId 변환 오류: {}", userIdString);
            throw new IllegalArgumentException("로그아웃 토큰 사용자 ID 형식이 유효하지 않습니다.");
        }

        refreshTokenRepository.deleteById(userId);

        log.info("로그아웃 완료 | userId={}", userId);
    }

    /**
     * userId 추출 헬퍼: principal 우선, 실패 시 AccessToken claims fallback
     */
    private Long extractUserId(Authentication authentication, TokenResponse tokenResponse) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            return cud.getUserId();
        }

        Claims claims = jwtTokenProvider.getClaims(tokenResponse.getAccessToken());
        String userIdString = claims.get("userId", String.class);
        if (userIdString == null || userIdString.isBlank()) {
            throw new IllegalStateException("JWT에 'userId' 클레임이 누락되었습니다.");
        }

        try {
            return Long.valueOf(userIdString);
        } catch (NumberFormatException e) {
            log.error("JWT userId 클레임 변환 오류: {}", userIdString);
            throw new IllegalStateException("JWT 사용자 ID 형식이 유효하지 않습니다.");
        }
    }
}
