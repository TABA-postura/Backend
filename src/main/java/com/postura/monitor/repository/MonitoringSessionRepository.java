package com.postura.monitor.repository;

import com.postura.monitor.entity.MonitoringSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoringSessionRepository extends JpaRepository<MonitoringSession, Long> {
}
