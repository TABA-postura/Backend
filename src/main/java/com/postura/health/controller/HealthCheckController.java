package com.postura.health.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ALB Health Check 전용 컨트롤러
 * /health 경로로 접근 시, 서버가 정상적으로 응답하는지 확인합니다.
 */
@RestController
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<Void> checkHealth() {
        // 서버가 정상적으로 응답할 경우 200 OK를 반환합니다.
        // 이 경로는 SecurityConfig에서 permitAll()로 설정하거나
        // ALB Health Check의 Success codes에 401을 추가해야 합니다.
        return ResponseEntity.ok().build();
    }
}