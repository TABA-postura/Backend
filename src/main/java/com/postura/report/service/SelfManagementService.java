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

    /**
     * 주간 리포트 데이터 조회 (하이브리드 로직)
     * - 그래프/추천: 최근 7일(Rolling) 데이터 사용 (끊김 방지)
     * - 요약 카드: 이번 주 월요일 ~ 오늘(Calendar) 데이터 사용
     */
    @Transactional(readOnly = true)
    public StatReportDto getWeeklyReport(Long userId, LocalDate referenceDate) {

        // 1. [그래프 & 추천용] 최근 7일 (오늘 포함 과거 6일 ~ 오늘)
        LocalDate rollingStart = referenceDate.minusDays(6);
        List<AggregateStat> rollingStats = aggregateStatRepository
                .findAllByUserIdAndStatDateBetweenOrderByStatDateAsc(userId, rollingStart, referenceDate);

        // 2. [요약 카드용] 이번 주 월요일 ~ 오늘
        LocalDate calendarMonday = referenceDate.with(DayOfWeek.MONDAY);
        List<AggregateStat> calendarWeekStats = aggregateStatRepository
                .findAllByUserIdAndStatDateBetweenOrderByStatDateAsc(userId, calendarMonday, referenceDate);

        // 3. [전주 대비 비교용] 지난 주 월요일 ~ 지난 주 일요일
        LocalDate lastMonday = calendarMonday.minusWeeks(1);
        LocalDate lastSunday = calendarMonday.minusDays(1);
        List<AggregateStat> lastWeekStats = aggregateStatRepository
                .findAllByUserIdAndStatDateBetweenOrderByStatDateAsc(userId, lastMonday, lastSunday);

        // 4. [달력용] 해당 월 전체 (1일 ~ 말일)
        LocalDate monthStart = referenceDate.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate monthEnd = referenceDate.with(TemporalAdjusters.lastDayOfMonth());
        List<AggregateStat> monthlyStats = aggregateStatRepository
                .findAllByUserIdAndStatDateBetweenOrderByStatDateAsc(userId, monthStart, monthEnd);

        // 데이터가 아예 없는 경우 처리
        if (rollingStats.isEmpty()) {
            throw new CustomException(ErrorCode.SESSION_NOT_FOUND, "조회된 통계 데이터가 없습니다.");
        }

        // --- 데이터 분석 및 가공 ---

        // A. 그래프 및 추천 (사용자 경험 연속성을 위해 Rolling 7일 기준)
        Map<String, Integer> rollingDistribution = calculatePostureDistribution(rollingStats);
        List<String> top3Issues = findTop3FrequentIssues(rollingDistribution);

        // B. 요약 카드 (기획 의도에 맞게 이번 주 월요일부터 기준)
        Double calendarAvgRatio = calculateAverageRatio(calendarWeekStats);
        Integer calendarTotalWarning = calculateTotalWarning(calendarWeekStats);

        // C. 전주 대비 변화율 계산 (이번 주 월~오늘 평균 vs 지난 주 월~일 평균)
        Double lastWeekAvg = calculateAverageRatio(lastWeekStats);
        Double ratioChangeVsLastWeek = calculateComparison(calendarAvgRatio, lastWeekAvg);

        // D. 최신 상태 (가장 최근 기록)
        AggregateStat latestStat = rollingStats.get(rollingStats.size() - 1);

        return StatReportDto.builder()
                // [그래프 데이터] 최근 7일치를 넘겨주어 월요일에도 그래프가 이어짐
                .dates(rollingStats.stream().map(AggregateStat::getStatDate).collect(Collectors.toList()))
                .correctRatios(rollingStats.stream().map(AggregateStat::getCorrectRatio).collect(Collectors.toList()))
                .warningCounts(rollingStats.stream().map(AggregateStat::getTotalWarningCount).collect(Collectors.toList()))

                // [요약 데이터] 월요일마다 갱신되는 "이번 주" 수치
                .currentAvgRatio(latestStat.getCorrectRatio()) // 오늘 수치
                .weeklyAvgRatio(calendarAvgRatio)              // 이번 주 월요일부터의 평균
                .weeklyTotalWarning(calendarTotalWarning)      // 이번 주 월요일부터의 총 경고
                .ratioChangeVsPreviousWeek(ratioChangeVsLastWeek) // 지난 주 대비 변화율

                // [기타 정보]
                .currentTotalWarning(latestStat.getTotalWarningCount())
                .currentConsecutiveAchievedDays(latestStat.getConsecutiveAchievedDays())
                .mostFrequentIssue(top3Issues.isEmpty() ? "GOOD" : top3Issues.get(0))

                // [분포 및 추천] 데이터가 풍부한 최근 7일 기준 분석 결과
                .postureDistribution(rollingDistribution)
                .recommendations(generateRecommendationsForTopIssues(top3Issues))

                // [달력] 한 달간의 성취도 목록
                .monthlyAchievements(monthlyStats.stream()
                        .map(s -> StatReportDto.CalendarAchievementDto.builder()
                                .date(s.getStatDate())
                                .ratio(s.getCorrectRatio())
                                .achieved(s.isGoalAchieved())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    // *************************************************************
    // 헬퍼 메서드들 (계산 로직)
    // *************************************************************

    /**
     * 두 평균값 간의 변화율(%)을 계산합니다.
     */
    private Double calculateComparison(Double currentAvg, Double previousAvg) {
        if (previousAvg == 0.0) {
            return currentAvg > 0.0 ? 100.0 : 0.0;
        }
        double change = ((currentAvg - previousAvg) / previousAvg) * 100.0;
        return Math.round(change * 100.0) / 100.0;
    }

    /**
     * 목록의 평균 유지율을 계산합니다.
     */
    private Double calculateAverageRatio(List<AggregateStat> stats) {
        if (stats == null || stats.isEmpty()) return 0.0;
        double avg = stats.stream()
                .mapToDouble(AggregateStat::getCorrectRatio)
                .average()
                .orElse(0.0);
        return Math.round(avg * 100.0) / 100.0;
    }

    /**
     * 목록의 총 경고 횟수를 합산합니다.
     */
    private Integer calculateTotalWarning(List<AggregateStat> stats) {
        if (stats == null) return 0;
        return stats.stream()
                .mapToInt(AggregateStat::getTotalWarningCount)
                .sum();
    }

    /**
     * 자세 유형별 총 발생 횟수를 계산합니다.
     */
    private Map<String, Integer> calculatePostureDistribution(List<AggregateStat> stats) {
        Map<String, Integer> distribution = new HashMap<>();
        for (AggregateStat stat : stats) {
            distribution.merge("FORWARD_HEAD", stat.getForwardHeadCount(), Integer::sum);
            distribution.merge("UNEQUAL_SHOULDERS", stat.getUnequalShouldersCount(), Integer::sum);
            distribution.merge("UPPER_BODY_TILT", stat.getUpperBodyTiltCount(), Integer::sum);
            distribution.merge("TOO_CLOSE", stat.getTooCloseCount(), Integer::sum);
            distribution.merge("ASYMMETRIC_POSTURE", stat.getAsymmetricPostureCount(), Integer::sum);
            distribution.merge("HEAD_TILT", stat.getHeadTiltCount(), Integer::sum);
            distribution.merge("LEANING_ON_ARM", stat.getLeaningOnArmCount(), Integer::sum);
        }
        return distribution.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 가장 빈번한 자세 불량 유형 Top 3를 추출합니다.
     */
    private List<String> findTop3FrequentIssues(Map<String, Integer> distribution) {
        return distribution.entrySet().stream()
                .filter(entry -> !entry.getKey().equalsIgnoreCase("GOOD") && entry.getValue() > 0)
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 문제 유형에 맞는 스트레칭 가이드를 무작위로 추천합니다.
     */
    private List<RecommendationDto> generateRecommendationsForTopIssues(List<String> top3ProblemTypes) {
        List<RecommendationDto> recommendations = new ArrayList<>();
        Random random = new Random();

        for (String problemType : top3ProblemTypes) {
            List<Content> guides = contentService.getGuidesByProblemType(problemType);
            if (!guides.isEmpty()) {
                Content randomGuide = guides.get(random.nextInt(guides.size()));
                recommendations.add(RecommendationDto.builder()
                        .problemType(problemType)
                        .recommendedGuideTitle(randomGuide.getTitle())
                        .guideId(randomGuide.getGuideId())
                        .build());
            }
        }
        return recommendations;
    }
}