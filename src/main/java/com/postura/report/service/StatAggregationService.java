package com.postura.report.service;

import com.postura.ai.entity.PostureLog;
import com.postura.ai.repository.PostureLogRepository;
import com.postura.monitor.repository.MonitoringSessionRepository;
import com.postura.report.entity.AggregateStat;
import com.postura.report.repository.AggregateStatRepository;
import com.postura.user.entity.User;
import com.postura.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatAggregationService {

    private final PostureLogRepository postureLogRepository;
    private final AggregateStatRepository aggregateStatRepository;
    private final UserRepository userRepository;
    private final MonitoringSessionRepository sessionRepository;

    // 목표 유지율 (80%)
    private static final double GOAL_RATIO = 80.0;

    // *************************************************************
    // 1. 메인 배치 실행 메서드
    // *************************************************************
    public void runDailyAggregation(LocalDate targetDate) {

        log.info("Starting daily aggregation for date: {}", targetDate);

        // 실제로는 페이징 처리 등을 하지만, 데모를 위해 모든 사용자 조회
        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            try {
                aggregateStatsForUser(user.getId(), targetDate);
            } catch (Exception e) {
                log.error("Error aggregating stats for user {}: {}", user.getId(), e.getMessage(), e);
                // 다음 사용자로 넘어갑니다. (트랜잭션은 사용자별로 분리되거나, 이 메서드 밖에서 관리되어야 함)
            }
        }
    }

    // *************************************************************
    // 2. 사용자별 통계 집계 로직 (핵심)
    // *************************************************************
    @Transactional
    public void aggregateStatsForUser(long userId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        // 1. 해당 날짜에 저장된 모든 자세 로그 조회
        List<PostureLog> logs = postureLogRepository.findAllByUserIdAndTimestampBetweenOrderByTimestampAsc(userId, start, end);

        if (logs.isEmpty()) {
            log.debug("No posture logs found for user {} on {}.", userId, date);
            return;
        }

        // 2. 집계 변수 초기화
        long totalGoodTime = 0;
        long totalAnalysisSeconds = 0;
        int totalWarningCount = 0;
        Map<String, Integer> postureCount = initializePostureCountMap(); // 7가지 자세 카운트를 위한 맵

        // 3. 로그 순회 및 지표 계산
        for (PostureLog log : logs) {
            // 모든 로그는 1초 간격으로 들어왔다고 가정하고 처리
            long duration = 1;
            totalAnalysisSeconds += duration;

            for (String state : log.getPostureStates()) {
                if ("Good".equalsIgnoreCase(state)) {
                    totalGoodTime += duration;
                } else if (!"UNKNOWN".equalsIgnoreCase(state)) {
                    totalWarningCount += 1; // 경고 횟수 증가
                    postureCount.merge(state, 1, Integer::sum); // 자세 유형별 카운트 증가
                }
            }
        }

        // 4. 유지율 계산 (세션 효율성 유지율)
        double maintenanceRatio = calculateMaintenanceRatio(totalGoodTime, totalAnalysisSeconds);

        // 5. 목표 달성 및 연속 달성일수 계산
        boolean goalAchieved = maintenanceRatio >= GOAL_RATIO;
        Optional<AggregateStat> existingStatOpt = aggregateStatRepository.findByUserIdAndStatDate(userId, date);
        int consecutiveDays = calculateConsecutiveAchievement(userId, date, goalAchieved);

        // 6. AggregateStat 엔티티 생성 및 저장 (Upsert)
        AggregateStat stat;

        if (existingStatOpt.isPresent()) {
            // [UPDATE] - 이미 통계가 존재함: 기존 엔티티를 가져와 필드를 갱신
            stat = existingStatOpt.get();

            stat.updateStats(
                    maintenanceRatio, totalWarningCount, totalAnalysisSeconds,
                    goalAchieved, consecutiveDays, postureCount
            );
        } else {
            // [INSERT] - 통계가 존재하지 않음: 새로운 엔티티 생성
            stat = AggregateStat.builder()
                    .user(userRepository.getReferenceById(userId))
                    .statDate(date)
                    .correctRatio(maintenanceRatio)
                    .totalWarningCount(totalWarningCount)
                    .totalAnalysisSeconds(totalAnalysisSeconds)
                    .goalAchieved(goalAchieved)
                    .consecutiveAchievedDays(consecutiveDays)
                    // 자세 유형별 카운트 매핑
                    .forwardHeadCount(postureCount.getOrDefault("FORWARD_HEAD", 0))
                    .unevenShoulderCount(postureCount.getOrDefault("UNE_SHOULDER", 0))
                    .upperTiltCount(postureCount.getOrDefault("UPPER_TILT", 0))
                    .tooCloseCount(postureCount.getOrDefault("TOO_CLOSE", 0))
                    .asymmetricCount(postureCount.getOrDefault("ASYMMETRIC", 0))
                    .headTiltCount(postureCount.getOrDefault("HEAD_TILT", 0))
                    .armLeanCount(postureCount.getOrDefault("ARM_LEAN", 0))
                    .build();
        }

        // 5. DB 저장 (트랜잭션 종료 시 UPDATE 또는 INSERT 쿼리 발생)
        aggregateStatRepository.save(stat);
        log.info("Stats UPSERT complete for user {} on {}. Ratio: {}%", userId, date, maintenanceRatio);
    }

    // *************************************************************
    // 3. 헬퍼 메서드
    // *************************************************************
    /**
     * 바른 자세 유지율을 계산합니다.
     */
    private double calculateMaintenanceRatio (long goodTime, long totalTime) {
        if (totalTime == 0) return 0;
        return Math.round((double) goodTime / totalTime * 10000.0) / 100.0; // 소수점 2자리 반올림
    }

    /**
     * 연속 목표 달성 일수를 계산합니다.
     */
    private int calculateConsecutiveAchievement(Long userId, LocalDate today, boolean goalAchievedToday) {
        if (!goalAchievedToday) {
            return 0; // 오늘 목표 실패 시 연속일수 0
        }

        // 어제 날짜
        LocalDate yesterday = today.minusDays(1);

        // 어제의 AggregateStat 기록 조회
        return aggregateStatRepository.findByUserIdAndStatDate(userId, yesterday)
                .filter(AggregateStat::isGoalAchieved) // 어제도 목표 달성했는지 확인
                .map(stat -> stat.getConsecutiveAchievedDays() + 1) // 어제 일수에 +1
                .orElse(1); // 어제 기록이 없거나 실패했다면 오늘이 1일째
    }

    /**
     * 자세 유형 카운트를 위한 맵을 초기화합니다.
     */
    private Map<String, Integer> initializePostureCountMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("FORWARD_HEAD", 0);
        map.put("UNE_SHOULDER", 0);
        map.put("UPPER_TILT", 0);
        map.put("TOO_CLOSE", 0);
        map.put("ASYMMETRIC", 0);
        map.put("HEAD_TILT", 0);
        map.put("ARM_LEAN", 0);
        return map;
    }
}
