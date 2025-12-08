package com.postura.auth.controller;

import com.postura.auth.service.AuthService;
import com.postura.dto.auth.LoginRequest;
import com.postura.dto.auth.RefreshTokenRequest;
import com.postura.dto.auth.SignUpRequest;
import com.postura.dto.auth.TokenResponse;
import com.postura.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * POST /api/auth/signup : 회원가입 API
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody @Valid SignUpRequest request) {
        userService.signUp(request);
        return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
    }

    /**
     * POST /api/auth/login : 로그인 API
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/reissue : 토큰 재발급 API
     */
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody @Valid RefreshTokenRequest request) {
        TokenResponse response = authService.reissue(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout : 로그아웃 API
     * - AccessToken을 받아서 토큰 블랙리스트 또는 RefreshToken 폐기
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        authService.logout(authorizationHeader);
        return ResponseEntity.ok("로그아웃이 완료되었습니다.");
    }
}
