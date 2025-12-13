package com.postura.ai.service;

import com.postura.ai.entity.PostureLog;
import com.postura.ai.repository.PostureLogRepository;
import com.postura.dto.ai.PostureLogRequest;
import com.postura.monitor.entity.MonitoringSession;
import com.postura.monitor.repository.MonitoringSessionRepository;
import com.postura.monitor.service.RealtimeFeedbackService;
import com.postura.user.entity.User;
import com.postura.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostureLogService {

    private final PostureLogRepository postureLogRepository;
    private final UserRepository userRepository;
    private final MonitoringSessionRepository sessionRepository;
    private final RealtimeFeedbackService realtimeFeedbackService;

    /**
     * FastAPI로부터 수신된 자세 로그를 처리하고 저장하는 핵심 메서드
     * DB 저장은 성능을 위해 비동기적으로 처리
     */

    @Async // 비동기 처리를 위한 annotation
    @Transactional
    public void processAndSaveLog (PostureLogRequest request) {

        // 1. MonitoringSession을 조회하여 DB에 저장된 안전한 User 객체(FK)를 가져옴
        MonitoringSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> {
                    log.error("Session not found for SessionId: {}", request.getSessionId());
                    return new IllegalArgumentException("Session not found: " + request.getSessionId());
                });

        // DB에 저장된 안전한 User 객체(userId)를 가져옵니다.
        User user = session.getUser();
        Long safeUserId = user.getId();

        // 2. DB 저장 조건 검사: "GOOD"이나 "UNKNOWN"이 아닌 자세가 하나라도 있는지 검사
        boolean hasWarningPosture = request.getPostureStates().stream()
                .anyMatch(state -> !state.equalsIgnoreCase("GOOD") && !state.equalsIgnoreCase("UNKNOWN"));

        // 3. 영구 저장 (RDS) - 조건부 실행
        if (hasWarningPosture) {
            // DB 부하 절감을 위해 비정상 자세일 경우에만 저장
            PostureLog newLog = PostureLog.from(request, user, session);
            postureLogRepository.save(newLog);
            log.debug("DB Saved: Warning log recorded for SessionId {}", request.getSessionId());
        } else {
            // 정상 자세일 경우 DB 저장 skip
            log.trace("DB Skip: Only 'Good' posture received for SessionId {}", request.getSessionId());
        }

        // 4. 실시간 피드백 업데이트 (Redis)
        // 최신 자세 상태를 Redis에 캐시하도록 monitor 모듈에 위임
        realtimeFeedbackService.updatePostureCache(
                safeUserId,
                request.getPostureStates());
    }
}
