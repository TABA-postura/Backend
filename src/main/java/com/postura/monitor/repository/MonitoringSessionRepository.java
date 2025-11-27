package com.postura.monitor.repository;

import com.postura.monitor.entity.MonitoringSession;
import com.postura.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonitoringSessionRepository extends JpaRepository<MonitoringSession, Long> {

    /**
     * 특정 사용자 ID와 세션 ID를 기준으로 활성 세션 조회
     * (보안상 userId와 sessionId를 함께 검증하는 것이 좋음)
     */
    Optional<MonitoringSession> findByIdAndUserId(Long sessionId, Long userId);

    Long user(User user);
}
