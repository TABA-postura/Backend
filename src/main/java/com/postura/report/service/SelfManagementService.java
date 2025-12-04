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

        // 1. 기간 내(이번 주) 모든 AggregateStat 조회
        List<AggregateStat> weeklyStats = aggregateStatRepository
                .findAllByUserIdAndStatDateBetweenOrderByStatDateAsc(userId, weekStart, weekEnd);

        if (weeklyStats.isEmpty()) {
            // 통계가 없는 경우, 사용자에게 Not Found 예외 반환 (프론트엔드는 기본 화면 표시)
            throw new CustomException(ErrorCode.SESSION_NOT_FOUND, "해당 기간의 통계 데이터가 없습니다.");
        }

        // 2. 지난 주 통계 조회 (전주 대비 변화율 계산을 위해)
        List<AggregateStat> previousWeeklyStats = getPreviousWeeklyStats(userId, weekStart);

        // 3. 데이터 가공 및 분석
        Map<String, Integer> distribution = calculatePostureDistribution(weeklyStats);
        List<String> top3FrequentIssues = findTop3FrequentIssues(distribution); // 가장 빈번한 불량 자세 top3

        // 4. 추가 지표 계산
        Double weeklyAvgRatio = calculateAverageRatio(weeklyStats);
        Integer weeklyTotalWarning = calculateTotalWarning(weeklyStats);
        Double ratioChangeVsPreviousWeek = calculateRatioChange(weeklyAvgRatio, previousWeeklyStats);

        // 3. DTO 빌드 및 반환
        AggregateStat latestStat = weeklyStats.get(weeklyStats.size() - 1);

        return StatReportDto.builder()
                // 그래프 데이터 구성
                .dates(weeklyStats.stream().map(AggregateStat::getStatDate).collect(Collectors.toList()))
                .correctRatios(weeklyStats.stream().map(AggregateStat::getCorrectRatio).collect(Collectors.toList()))
                .warningCounts(weeklyStats.stream().map(AggregateStat::getTotalWarningCount).collect(Collectors.toList()))

                // 요약 데이터 (가장 최근 기록을 사용)
                .currentAvgRatio(latestStat.getCorrectRatio())
                .weeklyAvgRatio(weeklyAvgRatio)
                .currentTotalWarning(latestStat.getTotalWarningCount())
                .currentConsecutiveAchievedDays(latestStat.getConsecutiveAchievedDays())
                .mostFrequentIssue(top3FrequentIssues.isEmpty() ? "Good" : top3FrequentIssues.get(0))

                .weeklyTotalWarning(weeklyTotalWarning) // 주간 합산 경고 횟수
                .ratioChangeVsPreviousWeek(ratioChangeVsPreviousWeek) // 전주 대비 변화율

                // 분포 및 추천
                .postureDistribution(distribution)
                .recommendations(generateRecommendationsForTopIssues(top3FrequentIssues))
                .build();
    }

    // *************************************************************
    // 2. 헬퍼 메서드 (분포 및 추천 로직)
    // *************************************************************

    /**
     * 지난 주의 통계 데이터를 조회합니다.
     */
    private List<AggregateStat> getPreviousWeeklyStats(Long userId, LocalDate currentWeekStart) {
        LocalDate previousWeekEnd = currentWeekStart.minusDays(1);
        LocalDate previousWeekStart = previousWeekEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        return aggregateStatRepository
                .findAllByUserIdAndStatDateBetweenOrderByStatDateAsc(userId, previousWeekStart, previousWeekEnd);
    }

    /**
     * 주간 통계 목록의 평균 유지율을 계산합니다.
     */
    private Double calculateAverageRatio(List<AggregateStat> stats) {
        if (stats.isEmpty()) return 0.0;

        double avg = stats.stream()
                .mapToDouble(AggregateStat::getCorrectRatio)
                .average()
                .orElse(0.0);

        // 소수점 둘째 자리까지 반올림
        return Math.round(avg * 100.0) / 100.0;
    }

    /**
     * 주간 통계 목록의 총 경고 횟수를 합산합니다.
     */
    private Integer calculateTotalWarning(List<AggregateStat> stats) {
        return stats.stream()
                .mapToInt(AggregateStat::getTotalWarningCount)
                .sum();
    }

    /**
     * 전주 대비 이번 주의 유지율 변화율(%)을 계산합니다.
     */
    private Double calculateRatioChange(Double currentAvg, List<AggregateStat> previousStats) {
        Double previousAvg = calculateAverageRatio(previousStats);

        if (previousAvg == 0.0) {
            return currentAvg > 0.0 ? 100.0 : 0.0; // 이전 주 통계가 없는데 이번 주 통계가 있으면 100% 증가로 간주
        }

        // (이번 주 평균 - 지난 주 평균) / 지난 주 평균 * 100
        double change = ((currentAvg - previousAvg) / previousAvg) * 100.0;

        // 소수점 둘째 자리까지 반올림
        return Math.round(change * 100.0) / 100.0;
    }

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
    private List<String> findTop3FrequentIssues(Map<String, Integer> distribution) {
        // "Good"이나 발생 횟수 0인 항목은 제외하고 내림차순 정렬 후 상위 3개만 추출
        return distribution.entrySet().stream()
                .filter(entry -> !entry.getKey().equalsIgnoreCase("Good") && entry.getValue() > 0)
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 가장 빈번한 자세 불량 유형을 기반으로 ContentService를 호출하여 스트레칭을 추천합니다.
     */
    private List<RecommendationDto> generateRecommendationsForTopIssues(List<String> top3ProblemTypes) {
        List<RecommendationDto> recommendations = new ArrayList<>();
        Random random = new Random();

        for (String problemType : top3ProblemTypes) {
            // 1. ContentService를 호출하여 해당 문제 유형과 관련된 모든 가이드 목록을 조회
            List<Content> guides = contentService.getGuidesByProblemType(problemType);

            if (guides.isEmpty()) {
                continue; // 가이드가 없으면 다음 문제 유형으로 이동
            }

            // 2. 무작위로 스트레칭 가이드 1개 선택
            Content randomGuide = guides.get(random.nextInt(guides.size()));

            // 3. RecommendationDto로 변환하여 목록에 추가
            recommendations.add(RecommendationDto.builder()
                    .problemType(problemType)
                    .recommendedGuideTitle(randomGuide.getTitle())
                    .guideId(randomGuide.getGuideId())
                    .build());
        }
        return recommendations;
    }
}
