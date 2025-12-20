package com.postura.ai.repository;

import com.postura.ai.entity.PostureLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PostureLog (자세 로그 엔티티)에 대한 DB 접근을 담당하는 Repository
 * JpaRepository를 상속받아 기본적인 CRUD 메서드를 자동으로 제공 받음
 */
@Repository
public interface PostureLogRepository extends JpaRepository<PostureLog, Long> {

    /*
    --------------기본 제공 메서드--------------
    save(PostureLog) : 로그 저장 (Controller -> Service -> Repository)
    findById(Long id) : 단일 로그 조회
    findAll() : 모든 로그 조회
    delete(PostureLog log) : 로그 삭제
    ------------------------------------------
     */

    List<PostureLog> findAllByUserIdAndTimestampBetweenOrderByTimestampAsc(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * 특정 시간 이전에 생성된 모든 로그를 삭제합니다. (로그 정리용)
     * @param limit 기준 시간
     * @return 삭제된 행(row)의 개수
     */
    long deleteByTimestampBefore(LocalDateTime limit);
}
