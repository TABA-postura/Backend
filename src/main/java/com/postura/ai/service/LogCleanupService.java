package com.postura.ai.service;

import com.postura.ai.repository.PostureLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 데이터베이스 성능 최적화를 위해 오래된 PostureLog를 주기적으로 삭제하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogCleanupService {

    private PostureLogRepository postureLogRepository;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupOldLogs() {

        // 현재 시간 기준 30일 이전 시간 계산 (정확히 30일치 데이터만 유지)
        LocalDateTime retentionLimit = LocalDateTime.now().minusDays(30);

        log.info("Starting daily rolling cleanup: Deleting posture logs created before {}", retentionLimit);

        try {
            // 리포지토리를 통해 조건에 맞는 데이터 삭제
            long deletedCount = postureLogRepository.deleteByTimestampBefore(retentionLimit);

            log.info("Daily cleanup completed. Total records deleted: {}", deletedCount);
        } catch (Exception e) {
            log.error("An error occurred during daily posture log cleanup: {}", e.getMessage(), e);
            // 배치 작업의 실패가 서비스 전체의 장애로 이어지지 않도록 예외 처리 후 로깅
        }
    }
}
