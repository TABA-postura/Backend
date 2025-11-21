package com.postura.dto.ai;

import lombok.Builder;
import lombok.Getter;

/**
 * React 프론트엔드로 전송되는 실시간 피드백 dto
 */
@Getter
@Builder
public class RealtimeFeedbackResponse {

    // 1. 현재 AI가 판단한 자세 상태 (예: "Good", "Forward_Head")
    private String currentPostureState;

    // 2. 사용자에게 표시할 코칭 메시지 (예: "훌륭합니다! 바른 자세를 유지하고 있습니다")
    private String feedbackMessage;

    // 3. 현재 시간 (프론트엔드 동기화 용)
    private String currentTime;
    
}
