package com.postura.dto.ai;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * FastAPI 서버로부터 실시간 자세 로그를 수신하기 위한 DTO
 */
@Getter
@ToString
@NoArgsConstructor // 매개변수 없는 기본 생성자 생성
public class PostureLogRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long sessionId;

    @NotNull
    private String postureStatus;

    @NotNull
    private LocalDateTime timestamp;

    // 상세 분석, 디버깅, AI 재학습 등을 위한 보조데이터 (NotNull 안붙임)
    // 전략 : JSON 문자열 형태로 전송받아 DB에는 TEXT로 저장
    private String landmarkData;
}
