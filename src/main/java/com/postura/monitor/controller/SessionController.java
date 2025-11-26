package com.postura.monitor.controller;

import com.postura.dto.monitor.SessionControlRequest;
import com.postura.dto.monitor.SessionStartResponse;
import com.postura.monitor.service.MonitoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // 편의를 위해 요청 본문에 sessionId와 userId를 포함하는 DTO 가정 (SessionControlRequest)

    /**
     * [POST /monitor/start] 모니터링 세션 시작 요청
     * (userId를 DTO 받음)
     */
    @PostMapping("/start")
    public ResponseEntity<SessionStartResponse> startSession(
            @Valid @RequestBody SessionControlRequest request)
    {
        log.info("Request to START session for UserId: {}", request.getUserId());

        SessionStartResponse response = monitoringService.startSession(request.getUserId());

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
        log.info("Request to PAUSE session Id: {} by UserId; {}", request.getSessionId(), request.getUserId());

        // sessionId와 userId를 DTO에서 직접 전달
        monitoringService.pauseSession(request.getSessionId(), request.getUserId());

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
        log.info("Request to RESUME session Id: {} by UserId; {}", request.getSessionId(), request.getUserId());

        // sessionId와 userId를 DTO에서 직접 전달
        monitoringService.resumeSession(request.getSessionId(), request.getUserId());

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
        log.info("Request to COMPLETE session Id: {} by UserId; {}", request.getSessionId(), request.getUserId());

        // sessionId와 userId를 DTO에서 직접 전달
        monitoringService.completeSession(request.getSessionId(), request.getUserId());

        // DB 상태 변경 완료 후 응답, React는 이 응답을 받고 AI reset 플래그와 함께 이미지 전송을 재개
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
