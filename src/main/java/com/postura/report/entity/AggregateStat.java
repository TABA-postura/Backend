package com.postura.report.entity;

import com.postura.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Map;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "aggregate_stat")
public class AggregateStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stat_id")
    private Long id;

    // 1. User와의 관계 설정 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 2. 통계 집계 기준 날짜 (ex. 2025-11-24)
    @Column(name = "stat_date", nullable = false, unique = true)
    private LocalDate statDate; // -> 그래프에 사용

    // 3. 통계 지표
    @Column(name = "correct_ratio", columnDefinition = "DECIMAL(5, 2)", nullable = false)
    private Double correctRatio; // 바른 자세 유지율 (%) -> 이번 주 평균 유지율, 자세 분석 그래프

    @Column(name = "total_warning_count", nullable = false)
    private Integer totalWarningCount; // 총 경고 횟수 (누적) -> 경고 횟수 (일/주), 일별 경고 횟수 그래프

    // 4. 총 분석 시간 (세션 총 합산 시간)
    @Column(name = "total_analysis_seconds", nullable = false)
    private Long totalAnalysisSeconds; // -> 해당 날짜의 유지율 계산의 분모

    // 5. 목표 달성 기록
    @Column(name = "goal_achieved", nullable = false)
    private boolean goalAchieved; // -> 해당 날짜 유지율 달성 여부

    // 6. 연속 목표 달성 일수 (유지율이 80% 이상일 경우 1일씩 증가)
    @Column(name = "consecutive_achieved_days", nullable = false)
    private Integer consecutiveAchievedDays;

    // *** 7. 자세 유형별 발생 횟수 (감지되는 7가지 자세 반영) ***
    @Column(name = "forward_head_count", nullable = false)
    private Integer forwardHeadCount; // 거북목

    @Column(name = "unequal_shoulders_count", nullable = false)
    private Integer unequalShouldersCount; // 한쪽 어깨 기울임

    @Column(name = "upper_body_tilt_count", nullable = false)
    private Integer upperBodyTiltCount; // 상체 기울임

    @Column(name = "too_close_count", nullable = false)
    private Integer tooCloseCount; // 화면과 너무 가까움

    @Column(name = "asymmetric_posture_count", nullable = false)
    private Integer asymmetricPostureCount; // 비대칭 자세

    @Column(name = "head_tilt_count", nullable = false)
    private Integer headTiltCount; // 머리 기울임

    @Column(name = "leaning_on_arm_count", nullable = false)
    private Integer LeaningOnArmCount; // 팔 지지 자세

    /**
     * 배치 작업 재실행 시 기존 AggregateStat 데이터를 새로운 값으로 갱신
     * @param postureCount 자세 유형별 발생 횟수를 담은 Map
     */
    public void updateStats(double correctRatio, int totalWarningCount, long totalAnalysisSeconds,
                            boolean goalAchieved, int consecutiveAchievedDays, Map<String, Integer> postureCount) {

        // 1. 주요 지표 업데이트
        this.correctRatio = correctRatio;
        this.totalWarningCount = totalWarningCount;
        this.totalAnalysisSeconds = totalAnalysisSeconds;
        this.goalAchieved = goalAchieved;
        this.consecutiveAchievedDays = consecutiveAchievedDays;

        // 2. 자세 유형별 카운트 업데이트
        this.forwardHeadCount = postureCount.getOrDefault("FORWARD_HEAD", 0);
        this.unequalShouldersCount = postureCount.getOrDefault("UNEQUAL_SHOULDERS", 0);
        this.upperBodyTiltCount = postureCount.getOrDefault("UPPER_BODY_TILT", 0);
        this.tooCloseCount = postureCount.getOrDefault("TOO_CLOSE", 0);
        this.asymmetricPostureCount = postureCount.getOrDefault("ASYMMETRIC_POSTURE", 0);
        this.headTiltCount = postureCount.getOrDefault("HEAD_TILT", 0);
        this.LeaningOnArmCount = postureCount.getOrDefault("LEANING_ON_ARM", 0);

        // 이 메서드 실행 후, @Transactional이 적용된 서비스에서 save()를 호출하면 JPA가 UPDATE 쿼리를 실행
    }
}
