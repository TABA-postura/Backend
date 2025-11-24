package com.postura.report.service;

import com.postura.common.exception.CustomException;
import com.postura.common.exception.ErrorCode;
import com.postura.content.entity.Content;
import com.postura.content.service.ContentService;
import com.postura.dto.report.RecommendationDto;
import com.postura.dto.report.StatReportDto;
import com.postura.report.entity.AggregateStat;
import com.postura.report.repository.AggregateStatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SelfManagementService {
    private final AggregateStatRepository aggregateStatRepository;
    private final ContentService contentService;

    // *************************************************************
    // 1. 주간 리포트 데이터 조회 (메인 메서드)
    // *************************************************************
    @Transactional(readOnly = true)
    public StatReportDto getWeeklyReport(Long userId, LocalDate weekStart) {

        // 주간 리포트 기간 설정: 주 시작일 ~ 오늘 (미래 조회 방지)
        LocalDate weekEnd = weekStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate today = LocalDate.now();

        if (weekEnd.isAfter(today)) {
            weekEnd = today; // 조회 기간이 미래를 넘지 않도록 조정
        }

        // 1. 기간 내 모든 AggregateStat 조회
        List<AggregateStat> weeklyStats = aggregateStatRepository
                .findAllByUserIdAndStatDateBetweenOrderByStatDateAsc(userId, weekStart, weekEnd);

        if (weeklyStats.isEmpty()) {
            // 통계가 없는 경우, 사용자에게 Not Found 예외 반환 (프론트엔드는 기본 화면 표시)
            throw new CustomException(ErrorCode.SESSION_NOT_FOUND, "해당 기간의 통계 데이터가 없습니다.");
        }

        // 2. 데이터 가공 및 분석
        Map<String, Integer> distribution = calculatePostureDistribution(weeklyStats);
        String mostFrequentIssue = findMostFrequentIssue(distribution);

        // 3. DTO 빌드 및 반환
        AggregateStat latestStat = weeklyStats.get(weeklyStats.size() - 1);

        return StatReportDto.builder()
                // 그래프 데이터 구성
                .dates(weeklyStats.stream().map(AggregateStat::getStatDate).collect(Collectors.toList()))
                .correctRatios(weeklyStats.stream().map(AggregateStat::getCorrectRatio).collect(Collectors.toList()))
                .warningCounts(weeklyStats.stream().map(AggregateStat::getTotalWarningCount).collect(Collectors.toList()))

                // 요약 데이터 (가장 최근 기록을 사용)
                .currentAvgRatio(latestStat.getCorrectRatio())
                .currentTotalWarning(latestStat.getTotalWarningCount())
                .currentConsecutiveAchievedDays(latestStat.getConsecutiveAchievedDays())
                .mostFrequentIssue(mostFrequentIssue)

                // 분포 및 추천
                .postureDistribution(distribution)
                .recommendations(generateRecommendations(mostFrequentIssue))
                .build();
    }

    // *************************************************************
    // 2. 헬퍼 메서드 (분포 및 추천 로직)
    // *************************************************************

    /**
     * 주간 통계를 합산하여 자세 유형별 총 발생 횟수를 계산합니다.
     */
    private Map<String, Integer> calculatePostureDistribution(List<AggregateStat> weeklyStats) {
        Map<String, Integer> distribution = new HashMap<>();

        // 모든 자세 유형 카운트를 초기화하고 집계
        for (AggregateStat stat : weeklyStats) {
            distribution.merge("FORWARD_HEAD", stat.getForwardHeadCount(), Integer::sum);
            distribution.merge("UNE_SHOULDER", stat.getUnevenShoulderCount(), Integer::sum);
            distribution.merge("UPPER_TILT", stat.getUpperTiltCount(), Integer::sum);
            distribution.merge("TOO_CLOSE", stat.getTooCloseCount(), Integer::sum);
            distribution.merge("ASYMMETRIC", stat.getAsymmetricCount(), Integer::sum);
            distribution.merge("HEAD_TILT", stat.getHeadTiltCount(), Integer::sum);
            distribution.merge("ARM_LEAN", stat.getArmLeanCount(), Integer::sum);
        }

        // 횟수가 0인 항목은 제외하고 반환 (파이 차트 데이터 구성 용이)
        return distribution.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 가장 빈번하게 발생한 자세 불량 유형을 찾습니다.
     */
    private String findMostFrequentIssue(Map<String, Integer> distribution) {
        // Map이 비어있다면 Good으로 간주
        if (distribution.isEmpty()) {
            return "Good";
        }
        return distribution.entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("Good");
    }

    /**
     * 가장 빈번한 자세 불량 유형을 기반으로 ContentService를 호출하여 스트레칭을 추천합니다.
     */
    private List<RecommendationDto> generateRecommendations(String problemType) {

        // 문제가 없으면 추천 목록도 비워둡니다.
        if ("Good".equals(problemType) || "UNKNOWN".equals(problemType)) {
            return Collections.emptyList();
        }

        // 1. ContentService를 호출하여 해당 문제 유형에 맞는 가이드 목록 조회
        List<Content> guides = contentService.getGuidesByProblemType(problemType);

        if (guides.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Content 엔티티를 RecommendationDto로 변환
        return guides.stream()
                .map(guide -> RecommendationDto.builder()
                        .problemType(problemType)
                        .recommendedGuideTitle(guide.getTitle())
                        .guideId(guide.getGuideId())
                        .build())
                .collect(Collectors.toList());
    }

}
