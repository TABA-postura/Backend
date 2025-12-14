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
     * provider = KAKAO | GOOGLE
     */
    @PostMapping("/oauth/{provider}")
    public ResponseEntity<TokenResponse> oauthLogin(
            @PathVariable AuthProvider provider,
            @RequestBody @Valid OAuthLoginRequest request
    ) {
        TokenResponse response =
                oAuthService.login(provider, request.getCode());
        return ResponseEntity.ok(response);
    }

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
