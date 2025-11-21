package com.postura.dto.monitor;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 모니터링 시작 요청 성공 시 클라이언트에 반환하는 응답 DTO
 */
@Getter
@AllArgsConstructor
public class SessionStartResponse {

    // 새로 생성된 모니터링 세션의 고유 ID
    private Long sessionId;

    // 세션 시작 시간 (클라이언트와 동기화를 위함)
    private String startTime;

}
