package com.postura.monitor.entity;

import com.postura.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access =  AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "monitoring_session")
public class MonitoringSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    // 1. User와의 관계 설정 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 2. 세션의 현재 상태 (Enum 매핑)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SessionStatus status;

    // 3. 실제 시간 기록 필드
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt; // 세션이 실제로 시작된 시각

    @Column(name = "end_at")
    private LocalDateTime endAt; // 세션이 실제로 종료된 시각

    // 4. 일시정지/재가동 시간 추적 필드 (총 분석 시간 계산에 사용)
    @Column(name = "paused_at")
    private LocalDateTime pausedAt; // 마지막으로 일시정지된 시각 (PAUSED 상태일 때 사용)

    // 5. 일시정지 시간까지의 누적 분석 시간 (초 단위)
    @Column(name = "accumulated_duration_seconds", nullable =  false)
    private Long accumulatedDurationSeconds;

    // *************************************************************
    // 추가 필드: Redis에서 읽어와 RDB에 영구 저장할 최종 카운터
    // *************************************************************

    @Column(name = "final_good_count")
    private Long finalGoodCount; // 최종 확정된 바른 자세 총 횟수 (유지율 분자)

    @Column(name = "final_total_count")
    private Long finalTotalCount; // 최종 확정된 총 로그 횟수 (유지율 분모)

    @Column(name = "final_warning_count")
    private Integer finalWarningCount; // 최종 확정된 총 경고 횟수

    // *********** 비즈니스 메서드 **************
    /**
     * 세션을 PAUSED 상태로 변경하고 현재까지의 누적 시간을 업데이트하는 메서드
     * (Service에서 호출)
     */
    public void pause(long currentDurationSeconds) {
        this.status = SessionStatus.PAUSED;
        this.pausedAt = LocalDateTime.now();
        // 이전 누적 시간에 현재 Running 시간을 더하여 최종 누적 시간 갱신
        this.accumulatedDurationSeconds += currentDurationSeconds;
    }

    /**
     * 세션을 STARTED 상태로 변경(재개)하는 메서드
     * (Service에서 호출)
     */
    public void resume() {
        // PAUSED 상태에서만 호출되어야 함 (Service 검증)
        this.status = SessionStatus.STARTED;
        this.pausedAt = LocalDateTime.now(); // 재개 시 pausedAt 초기화
        // accumulatedDurationSeconds는 그대로 유지함
    }

    /**
     * 세션을 COMPLETED 상태로 변경하고 최종 종료 시각 기록
     */
    public void complete(long finalDurationSeconds, Long finalGoodCount, Long finalTotalCount, Integer finalWarningCount) {
        // 최종 누적 시간에 마지막 Running 시간을 더하여 총 분석 시간 확정
        this.accumulatedDurationSeconds += finalDurationSeconds;
        this.status = SessionStatus.COMPLETED;
        this.endAt = LocalDateTime.now();

        this.finalGoodCount = finalGoodCount;
        this.finalTotalCount = finalTotalCount;
        this.finalWarningCount = finalWarningCount;
    }
}
