package com.postura.monitor.service;

import com.postura.common.exception.CustomException;
import com.postura.common.exception.ErrorCode;
import com.postura.dto.monitor.SessionStartResponse;
import com.postura.monitor.entity.MonitoringSession;
import com.postura.monitor.entity.SessionStatus;
import com.postura.monitor.repository.MonitoringSessionRepository;
import com.postura.report.service.StatAggregationService;
import com.postura.user.entity.User;
import com.postura.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

    private final MonitoringSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final StatAggregationService  statAggregationService;
    private final RealtimeFeedbackService realtimeFeedbackService;

    /**
     * 세션 시작 (START)
     * @param userId
     * @return SessionStartResponse
     */
    @Transactional
    public SessionStartResponse startSession(Long userId) {
        // 1. User 조회 및 유효성 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 새로운 세션 시작 전, 이전 세션의 Redis 캐시 데이터 초기화 (누적 통계 0으로 리셋)
        realtimeFeedbackService.clearUserCache(userId);

        // 3. MonitoringSession 엔티티 생성 및 DB 저장 (STARTED 상태)
        MonitoringSession session = MonitoringSession.builder()
                .user(user)
                .status(SessionStatus.STARTED)
                .startAt(LocalDateTime.now())
                .accumulatedDurationSeconds(0L)
                .finalGoodCount(null)
                .finalTotalCount(null)
                .finalWarningCount(null)
                .build();
        session =  sessionRepository.save(session);

        // 4. React에 SessionStartResponse 반환
        // (React는 해당 응답을 받은 후 reset=true 플래그와 함께 FastAPI에 이미지 전송)
        log.info("Session STARTED: UserId={}, SessionId={}", userId, session.getId());
        return new SessionStartResponse(session.getId(), session.getStartAt().toString());
    }

    /**
     * 일시 정지 (PAUSED)
     * @param sessionId
     * @param userId
     */
    @Transactional
    public void pauseSession (Long sessionId, Long userId) {
        MonitoringSession session = getSession(sessionId,userId);

        if (session.getStatus() != SessionStatus.STARTED) {
            throw new CustomException(ErrorCode.INVALID_SESSION_STATUS, "PAUSE는 STARTED 상태에서만 가능합니다.");
        }

        // 1. 현재 진행 시간 계산 (SECONDS)
        // pausedAt이 Null이면 startAt을 기준으로, 아니면 pausedAt을 기준으로 현재까지의 시간 계산
        long currentRunningDuration = calculateRunningDuration(session);

        // 2. Entity 업데이트 및 DB 저장 (PAUSED 상태로 변경)
        session.pause(currentRunningDuration);
        sessionRepository.save(session);

        // 3. AI 로그 전송 중단 명령 없음 (React가 이미지 전송을 멈추면 FastAPI가 스스로 중단함)
        log.info("Session PAUSED: SessionId={}, Accumulated Seconds: {}", sessionId, session.getAccumulatedDurationSeconds());
    }

    /**
     * 재개 (RESUME)
     * @param sessionId
     * @param userId
     */
    @Transactional
    public void resumeSession (Long sessionId, Long userId) {
        MonitoringSession session = getSession(sessionId,userId);

        if (session.getStatus() != SessionStatus.PAUSED) {
            throw new CustomException(ErrorCode.INVALID_SESSION_STATUS, "RESUME은 PAUSED 상태에서만 가능합니다.");
        }

        // 1. Entity 상태 변경 및 DB 저장 (STARTED 상태로 복귀)
        // DB 트랜잭션 무결성 보장
        session.resume();
        sessionRepository.save(session);

        // 2. React가 성공 응답 받은 후 reset=true 플래그와 이미지를 보냄
        log.info("Session RESUME: SessionId={}, Status set to STARTED", sessionId);
    }

    /**
     * 종료 (COMPLETED)
     * @param sessionId
     * @param userId
     */
    @Transactional
    public void completeSession (Long sessionId, Long userId) {
        MonitoringSession session = getSession(sessionId,userId);

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new CustomException(ErrorCode.INVALID_SESSION_STATUS, "이미 종료된 세션입니다.");
        }

        // 1. 최종 진행 시간 계산
        long lastRunningDuration = 0;
        if (session.getStatus() == SessionStatus.STARTED || session.getStatus() == SessionStatus.PAUSED) {
            if (session.getStatus() == SessionStatus.STARTED) {
                lastRunningDuration = calculateRunningDuration(session);
            }
            // 2. Redis에서 최종 카운트 조회
            Map<String, Long> finalCounts = realtimeFeedbackService.getFinalSessionCounts(userId);
            Long finalGood = finalCounts.getOrDefault("finalGoodCount", 0L);
            Long finalTotal = finalCounts.getOrDefault("finalTotalCount", 0L);
            Integer finalWarning = finalCounts.getOrDefault("finalWarningCount", 0L).intValue();

            // 진단 로그 추가: Redis에서 가져온 카운트 확인
            log.info("Redis Final Counts: Total={}, Good={}, Warning={}", finalTotal, finalGood, finalWarning);

            // 3. Entity 최종 업데이트 및 DB 저장 (COMPLETED 상태로 변경)
            session.complete(lastRunningDuration, finalGood, finalTotal, finalWarning);
            sessionRepository.save(session);

            // 4. 오늘 날짜 통계 즉시 업데이트 로직
            try {
                LocalDate today = LocalDate.now();
                // 해당 사용자의 오늘 통계만 즉시 재계산 및 업데이트 (UPSERT)
                statAggregationService.aggregateStatsForUser(userId, today);
                log.info("On-demand stats update complete for user {} on {}.", userId, today);

                // 5. Redis 캐시 정리 - 세션 완료 후 캐시를 삭제하여 데이터 유출 방지
                realtimeFeedbackService.clearUserCache(userId);

            } catch (Exception e) {
                // 통계 집계 실패는 세션 종료 자체를 막아서는 안 됨 (로그만 남김)
                log.error("Failed to run on-demand aggregation after session completion: {}", e.getMessage());
            }
        }
        // 6. React가 성공 응답 받은 후 이미지 전송 멈춤
        log.info("Session COMPLETED: SessionId={}. Total Duration: {}", sessionId, session.getAccumulatedDurationSeconds());
    }

    // ************* 유틸리티 메서드 *************

    /**
     * Session ID와 user ID를 사용하여 유효한 세션 엔티티 조회
     * @param sessionId
     * @param userId
     * @return MonitoringSession
     */
    private MonitoringSession getSession (Long sessionId, Long userId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));
    }

    /**
     * 마지막 기록된 시간부터 현재까지의 진행 시간을 계산
     * @param session
     * @return long
     */
    private long calculateRunningDuration(MonitoringSession session) {
        LocalDateTime startTime = session.getPausedAt() != null
                ? session.getPausedAt()
                : session.getStartAt();
        if (startTime == null) return 0; // 시작 시간이 없는 경우

        // 시작 시간과 현재 시각의 차이를 초 단위로 반환
        return ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
    }
}
