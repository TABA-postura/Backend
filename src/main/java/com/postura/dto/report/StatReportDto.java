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
    private final List<LocalDate> dates; // 주간 추이 그래프의 날짜(요일) 축을 구성
    private final List<Double> correctRatios; // 그래프의 유지율 라인 데이터를 구성
    private final List<Integer> warningCounts; // 일별 경고 횟수 막대 그래프 데이터를 구성

    // 2. 가장 최근 데이터 요약 (대시보드 상단 카드)
    private final Double currentAvgRatio; // 금일 바른 자세 유지율
    private final Double weeklyAvgRatio; // 이번주 평균 바른 자세 유지율
    private final Double ratioChangeVsPreviousWeek; // 이번 주 유지율 평균과 전주 유지율 평균의 변화율
    private final Integer currentTotalWarning; // 경고 횟수 (오늘)
    private final Integer weeklyTotalWarning; // 이번 주 총 경고 횟수
    private final Integer currentConsecutiveAchievedDays; // 최신 연속 목표 달성 일수
    private final String mostFrequentIssue; // 가장 빈번한 나쁜 자세 유형

    // 3. 자세 분포 비율 (파이 차트)
    private final Map<String, Integer> postureDistribution; // ex: {"FORWARD_HEAD": 30, "UPPER_TILT": 10, ...}

    // 4. 맞춤 스트레칭 추천
    private final List<RecommendationDto> recommendations; // 추천 스트레칭 목록

    private final List<CalendarAchievementDto> monthlyAchievements;

    @Getter
    @Builder
    public static class CalendarAchievementDto {
        private final LocalDate date;
        private final Double ratio;
        private final boolean achieved; // 80% 이상 여부
    }
}
