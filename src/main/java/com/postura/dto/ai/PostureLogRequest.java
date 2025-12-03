package com.postura.dto.ai;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<String> postureStates;

    @NotNull
    private LocalDateTime timestamp;

}
