package com.postura.dto.monitor;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모니터링 세션 제어 요철 (START, PAUSE, RESUME, COMPLETE)에 사용되는 DTO
 * (임시로 userId를 포함 시킴, 추후 JWT로 변경 예정)
 */
@Getter
@NoArgsConstructor
public class SessionControlRequest {

    @NotNull
    private Long sessionId;

}
