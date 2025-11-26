package com.postura.report.repository;

import com.postura.report.entity.AggregateStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AggregateStatRepository extends JpaRepository<AggregateStat, Long> {

    /**
     * 특정 사용자의 특정 날짜에 대한 집계 통계를 조회
     * 배치 처리 시, 해당 날짜에 이미 통계가 있는지 확인하는 데 사용
     */
    Optional<AggregateStat> findByUserIdAndStatDate(Long userId, LocalDate statDate);

    /**
     * 주간 리포트 조회를 위해 특정 사용자 ID와 기간 내의 모든 통계를 조회
     * 통계 데이터는 날짜 순으로 정렬됨
     *
     * @param userId 사용자 ID
     * @param startDate 조회 시작 날짜 (포함)
     * @param endDate 조회 종료 날짜 (포함)
     * @return 해당 기간의 AggregateStat 리스트
     */
    List<AggregateStat> findAllByUserIdAndStatDateBetweenOrderByStatDateAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * 연속 목표 달성 일수를 계산하기 위해, 특정 날짜 이전의 가장 최근 통계 데이터를 조회
     */
    Optional<AggregateStat> findTopByUserIdAndStatDateBeforeOrderByStatDateDesc(
            Long userId,
            LocalDate statDate
    );
}
