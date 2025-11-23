package com.postura.monitor.controller;

import com.postura.dto.ai.RealtimeFeedbackResponse;
import com.postura.monitor.service.RealtimeFeedbackService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final RealtimeFeedbackService realtimeFeedbackService;

    /**
     * [GET /monitor/feedback] 실시간 자세 피드백 폴링 요청
     * React가 1초마다 최신 자세 상태 및 코칭 메시지를 조회
     * ***임시 방편으로 userId를 쿼리 파라미터로 받음 (추후 JWT로 변경 예정)***
     * @param userId
     * @return RealtimeFeedbackResponse
     */
    @GetMapping("/feedback")
    public ResponseEntity<RealtimeFeedbackResponse> getRealtimeFeedback(
            // 쿼리 파라미터로 userId를 받도록 임시 가정
            @RequestParam @NotNull Long userId
    ) {
        // 1. 서비스 로직 위임: Redis에서 최신 캐시 데이터를 조회하여 DTO 생성
        RealtimeFeedbackResponse response = realtimeFeedbackService.getRealtimeFeedback(userId);

        log.debug("Feedback Sent to UserId {}: {}", userId, response.getCurrentPostureStates());

        return ResponseEntity.ok(response);
    }
}
