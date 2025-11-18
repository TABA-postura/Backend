package com.postura.auth.controller;

import com.postura.auth.service.AuthService;
import com.postura.dto.auth.LoginRequest;
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

    private final UserService userService; // 회원가입 처리 (UserService에 로직 위임)
    private final AuthService authService; // 로그인/토큰 처리 (AuthService에 로직 위임)

    /**
     * POST /api/auth/signup : 회원가입 API
     * @param request 이메일, 비밀번호, 이름
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody @Valid SignUpRequest request) {
        // 비즈니스 로직은 UserService에 위임
        userService.SignUp(request);
        return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
    }

    /**
     * POST /api/auth/login : 로그인 API
     * @param request 이메일, 비밀번호
     * @return Access Token과 Refresh Token
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        // 인증 및 토큰 발급 로직은 AuthService에 위임
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * TODO: POST /api/auth/reissue : 토큰 재발급 API 구현 예정
     * 클라이언트로부터 Refresh Token을 받아 새로운 Access Token을 발급합니다.
     */
}