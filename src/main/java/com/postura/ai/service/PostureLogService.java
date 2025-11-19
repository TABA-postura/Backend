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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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

        // 1. DTO에 포함된 ID를 통해 User와 Session 엔티티 조회 -> Fk 연결
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));
        MonitoringSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + request.getSessionId()));

        // 2. DTO -> Entity 변환
        PostureLog newLog = PostureLog.from(request, user, session);

        // 3. 영구 저장 (RDS)
        postureLogRepository.save(newLog);

        // 4. 실시간 피드백 업데이트 (Redis)
        // 최신 자세 상태를 Redis에 캐시하도록 monitor 모듈에 위임
        realtimeFeedbackService.updatePostureCache(
                request.getUserId(),
                request.getPostureStatus(),
                request.getLandmarkData());

    }
}
