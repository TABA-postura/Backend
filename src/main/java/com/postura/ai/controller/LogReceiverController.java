package com.postura.ai.controller;

import com.postura.ai.entity.PostureLog;
import com.postura.ai.service.PostureLogService;
import com.postura.dto.ai.PostureLogRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 핵심 역할
 * 1. 요청 수신 : FastAPI로부터 HTTP POST 요청 수신
 * 2. 유효성 검사 : 수신된 PostureLogRequest DTO의 @NotNull 제약 조건 검증
 * 3. 로직 위임 : 수신된 데이터 PostureLogService에 전달 -> 데이터 처리 & 저장 위임
 * 4. 즉시 응답 : 컨트롤러는 즉시 성공 응답 반환 (FastAPI 서버의 지연 최소화)
 * 5. 접근 권한 : 해당 엔드포인트는 외부 서비스(FastAPI)가 사용 -> SecurityConfig에서 permitAll()로 설정
 */

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class LogReceiverController {

    private final PostureLogService postureLogService;

    /**
     * [엔드포인트] : POST /ai/log
     * FastAPI로 부터 실시간 자세 로그를 수신
     * @param request (PostureLogRequest)
     * @return 202 Accepted (비동기 처리 후 즉시 응답
     */
    public ResponseEntity<Void> receivePostureLog(
        // @Vaild 사용 -> DTO의 NotNull 제약 조건 검사
        @Valid @RequestBody PostureLogRequest request)
    {

        // 1. 서비스 계층에 비동기 로직 위임 (DB 저장 및 Redis 업데이트)
        postureLogService.processAndSaveLog(request);

        // 2. FastAPI 서버에 즉시 응답
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
