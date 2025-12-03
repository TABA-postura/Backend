package com.postura.monitor.controller;

import com.postura.common.exception.CustomException;
import com.postura.common.exception.ErrorCode;
import com.postura.dto.monitor.SessionControlRequest;
import com.postura.dto.monitor.SessionStartResponse;
import com.postura.monitor.service.MonitoringService;
import com.postura.user.service.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final MonitoringService monitoringService;

    // *************************************************************
    // JWT 인증된 사용자 ID를 SecurityContext에서 추출하는 헬퍼 메서드
    // *************************************************************
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증되지 않았거나 익명 사용자인 경우 (JwtAuthenticationFilter 통과 실패 시)
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();

        // CustomUserDetails 객체에서 userId 추출
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUserId();
        }

        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "인증된 사용자 ID 추출 실패: Principal 타입 불일치");
    }

    /**
     * [POST /monitor/start] 모니터링 세션 시작 요청
     * (userId를 DTO 받음)
     */
    @PostMapping("/start")
    public ResponseEntity<SessionStartResponse> startSession(
            @Valid @RequestBody SessionControlRequest request)
    {
        Long userId = getAuthenticatedUserId(); // 💡 JWT에서 userId 추출
        log.info("Request to START session for UserId: {}", userId);

        SessionStartResponse response = monitoringService.startSession(userId);

        // DB 트랜잭션 완료 후, React는 이 응답을 받고 AI Reset 플래그와 함께 이미지 전송
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * [POST /monitor/pause] 모니터링 세션 일시 정지 요철
     */
    @PostMapping("/pause")
    public ResponseEntity<Void> pauseSession(
            @Valid @RequestBody SessionControlRequest request)
    {
        Long userId = getAuthenticatedUserId(); // JWT에서 userId 추출
        log.info("Request to PAUSE session Id: {} by UserId: {}", request.getSessionId(), userId);

        // sessionId를 DTO에서 직접 전달
        monitoringService.pauseSession(request.getSessionId(), userId);

        // DB 상태 변경 완료 후 응답, React는 이 응답을 받고 이미지 전송을 멈춤
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * [POST /monitor/resume] 모니터링 세션 재개 요철
     */
    @PostMapping("/resume")
    public ResponseEntity<Void> resumeSession(
            @Valid @RequestBody SessionControlRequest request)
    {
        Long userId = getAuthenticatedUserId(); // JWT에서 userId 추출
        log.info("Request to RESUME session Id: {} by UserId: {}", request.getSessionId(), userId);

        // sessionId를 DTO에서 직접 전달
        monitoringService.resumeSession(request.getSessionId(), userId);

        // DB 상태 변경 완료 후 응답, React는 이 응답을 받고 AI reset 플래그와 함께 이미지 전송을 재개
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * [POST /monitor/complete] 모니터링 세션 종료 요철
     */
    @PostMapping("/complete")
    public ResponseEntity<Void> completeSession(
            @Valid @RequestBody SessionControlRequest request)
    {
        Long userId = getAuthenticatedUserId(); // JWT에서 userId 추출
        log.info("Request to COMPLETE session Id: {} by UserId: {}", request.getSessionId(), userId);

        // sessionId를 직접 전달
        monitoringService.completeSession(request.getSessionId(), userId);

        // DB 상태 변경 완료 후 응답, React는 이 응답을 받고 AI reset 플래그와 함께 이미지 전송을 중지
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
