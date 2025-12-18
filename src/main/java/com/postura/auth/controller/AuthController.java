package com.postura.auth.controller;

import com.postura.auth.service.AuthService;
import com.postura.dto.auth.LoginRequest;
import com.postura.dto.auth.RefreshTokenRequest;
import com.postura.dto.auth.SignUpRequest;
import com.postura.dto.auth.TokenResponse;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API (LOCAL 로그인/회원가입/재발급/로그아웃)
 * - OAuth2(Kakao/Google)는 Spring Security oauth2Login + Success/FailureHandler에서 처리
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/signup : 회원가입 (LOCAL)
     * - 성공 시 200 + JSON 반환
     * - 중복 이메일은 GlobalExceptionHandler에서 409로 변환됨
     */
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signUp(@RequestBody @Valid SignUpRequest request) {
        authService.signUp(request);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("회원가입이 성공적으로 완료되었습니다.")
                .build());
    }

    /**
     * POST /api/auth/login : 로그인 (LOCAL)
     * - 성공: TokenResponse 반환
     * - 실패(인증 실패): GlobalExceptionHandler에서 401로 변환됨
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /api/auth/reissue : 토큰 재발급
     * - 실패(RefreshToken 불량/불일치): 401로 변환 권장(현재 AuthService에서 BadCredentialsException 사용)
     */
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.reissue(request.getRefreshToken()));
    }

    /**
     * POST /api/auth/logout : 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestHeader("Authorization") String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("로그아웃이 완료되었습니다.")
                .build());
    }

    @Getter
    @Builder
    public static class MessageResponse {
        private final String message;
    }
}
