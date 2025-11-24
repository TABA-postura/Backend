package com.postura.report.entity;

import com.postura.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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
    private LocalDate statData;

    // 3. 통계 지표
    @Column(name = "correct_ratio", precision = 5, scale = 2, nullable = false)
    private Double correctRatio; // 바른 자세 유지율 (%)

    @Column(name = "total_warning_count", nullable = false)
    private Integer totalWarningCount; // 총 경고 횟수 (누적)

    // 4. 총 분석 시간 (세션 총 합산 시간)
    @Column(name = "total_analysis_seconds", nullable = false)
    private Integer totalAnalysisSeconds;

    // 5. 목표 달성 기록
    @Column(name = "goal_achieved", nullable = false)
    private boolean goalAchieved;

    // 6. 연속 목표 달성 일수 (유지율이 80% 이상일 경우 1일씩 증가)
    @Column(name = "consecutive_achieved_days", nullable = false)
    private Integer consecutiveAchievedDays;

    // *** 7. 자세 유형별 발생 횟수 (감지되는 7가지 자세 반영) ***
    @Column(name = "forward_head_count", nullable = false)
    private Integer forwardHeadCount; // 거북목

    @Column(name = "uneven_shoulder_count", nullable = false)
    private Integer unevenShoulderCount; // 한쪽 어깨 기울임

    @Column(name = "upper_tilt_count", nullable = false)
    private Integer upperTiltCount; // 상체 기울임

    @Column(name = "too_close_count", nullable = false)
    private Integer tooCloseCount; // 화면과 너무 가까움

    @Column(name = "asymmetric_count", nullable = false)
    private Integer asymmetricCount; // 비대칭 자세

    @Column(name = "head_tilt_count", nullable = false)
    private Integer headTiltCount; // 머리 기울임

    @Column(name = "arm_lean_count", nullable = false)
    private Integer armLeanCount; // 팔 지지 자세

}
