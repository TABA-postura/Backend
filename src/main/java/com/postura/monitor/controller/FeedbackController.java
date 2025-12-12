package com.postura.monitor.controller;

import com.postura.common.exception.CustomException;
import com.postura.common.exception.ErrorCode;
import com.postura.dto.ai.RealtimeFeedbackResponse;
import com.postura.monitor.service.RealtimeFeedbackService;
import com.postura.user.service.CustomUserDetails;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final RealtimeFeedbackService realtimeFeedbackService;

    // *************************************************************
    // JWT 인증된 사용자 ID를 SecurityContext에서 추출하는 헬퍼 메서드
    // *************************************************************
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증되지 않았거나 익명 사용자(JWT 검증 실패)인 경우 처리
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
     * [GET /api/monitor/feedback] 실시간 자세 피드백 폴링 요청
     * React가 1초마다 최신 자세 상태 및 코칭 메시지를 조회
     * @return RealtimeFeedbackResponse
     */
    @GetMapping("/feedback")
    public ResponseEntity<RealtimeFeedbackResponse> getRealtimeFeedback() {
        // 1. JWT에서 인증된 userId 획득 (쿼리 파라미터 대신 사용)
        Long userId = getAuthenticatedUserId();

        // 2. 서비스 로직 위임: Redis에서 최신 캐시 데이터를 조회하여 DTO 생성
        RealtimeFeedbackResponse response = realtimeFeedbackService.getRealtimeFeedback(userId);

        log.debug("Feedback Sent to UserId {}: {}", userId, response.getCurrentPostureStates());

        return ResponseEntity.ok(response);
    }
}
