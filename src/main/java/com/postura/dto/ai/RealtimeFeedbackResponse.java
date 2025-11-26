package com.postura.dto.ai;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * React 프론트엔드로 전송되는 실시간 피드백 dto
 */
@Getter
@Builder
public class RealtimeFeedbackResponse {

    // 1. 현재 AI가 판단한 자세 상태 (예: "Good", "Forward_Head")
    private List<String> currentPostureStates;

    // 2. 사용자에게 표시할 코칭 메시지 (예: "훌륭합니다! 바른 자세를 유지하고 있습니다")
    private List<String> feedbackMessages;

    // 3. 현재 시간 (프론트엔드 동기화 용)
    private String currentTime;

    // 4. 실시간 통계 데이터
    private final Double correctPostureRatio; // 바른 자세 유지율

    private final Integer totalWarningCount; // 경고 횟수

    // 5. 세션 내 자세 유형별 누적 횟수 (누적 자세 데이터)
    private final Map<String, Integer> postureTypeCounts;
    
}
