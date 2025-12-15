package com.postura.auth.controller;

import com.postura.auth.service.AuthService;
import com.postura.auth.service.OAuthService;
import com.postura.dto.auth.LoginRequest;
import com.postura.dto.auth.OAuthLoginRequest;
import com.postura.dto.auth.RefreshTokenRequest;
import com.postura.dto.auth.SignUpRequest;
import com.postura.dto.auth.TokenResponse;
import com.postura.user.entity.User.AuthProvider;
import com.postura.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final OAuthService oAuthService;

    /**
     * POST /api/auth/signup : 회원가입 (LOCAL)
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody @Valid SignUpRequest request) {
        userService.signUp(request);
        return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
    }

    /**
     * POST /api/auth/login : 로그인 (LOCAL)
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /api/auth/oauth/{provider} : 소셜 로그인
     *
     * 🚨 삭제/주석 처리: 이 API는 Spring Security OAuth2 Success Handler와 충돌하며,
     * OAuthService.login 메서드의 파라미터 불일치 오류를 일으킵니다.
     * SecurityConfig의 Success Handler가 이 역할을 대신 수행합니다.
     */
    // @PostMapping("/oauth/{provider}")
    // public ResponseEntity<TokenResponse> oauthLogin(
    //         @PathVariable AuthProvider provider,
    //         @RequestBody @Valid OAuthLoginRequest request
    // ) {
    //     // 이 부분의 oAuthService.login 호출이 String을 UserInfo로 변환할 수 없어 오류를 일으켰습니다.
    //     // Spring Security OAuth2 플로우를 따르기 위해 이 메서드는 불필요하므로 제거합니다.
    //     return null;
    // }


    /**
     * POST /api/auth/reissue : 토큰 재발급
     */
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.reissue(request.getRefreshToken()));
    }

    /**
     * POST /api/auth/logout : 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        authService.logout(authorizationHeader);
        return ResponseEntity.ok("로그아웃이 완료되었습니다.");
    }
}