package com.postura.dto.report;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class StatReportDto {

    // 1. 주간 추이 데이터 (그래프)
    private final List<LocalDate> dates;
    private final List<Double> correctRatios; // 바른 자세 유지율 추이
    private final List<Integer> warningCounts; // 경고 횟수 추이

    // 2. 가장 최근 데이터 요약 (대시보드 상단 카드)
    private final Double currentAvgRatio; // 최신 바른 자세 유지율 (%)
    private final Integer currentTotalWarning; // 최신 총 경고 횟수
    private final Integer currentConsecutiveAchievedDays; // 최신 연속 목표 달성 일수
    private final String mostFrequentIssue; // 가장 빈번한 나쁜 자세 유형

    // 3. 자세 분포 (파이 차트)
    private final Map<String, Integer> postureDistribution; // ex: {"FORWARD_HEAD": 30, "UPPER_TILT": 10, ...}

    // 4. 맞춤 스트레칭 추천
    private final List<RecommendationDto> recommendations; // 추천 스트레칭 목록
}
