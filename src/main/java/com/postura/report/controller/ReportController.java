package com.postura.report.controller;

import com.postura.common.exception.CustomException;
import com.postura.common.exception.ErrorCode;
import com.postura.dto.report.StatReportDto;
import com.postura.report.service.SelfManagementService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final SelfManagementService selfManagementService;

    @GetMapping("/weekly")
    public ResponseEntity<StatReportDto> getWeeklyReport(
            @RequestParam @NotNull Long userId,
            @RequestParam(required = false) String weekStart)
    {
        // 1. userId는 쿼리 파라미터에서 임의로 직접 획득 (JWT 로직 제외)

        // 2. 주간 시작일 결정 : 파라미터가 없으면 오늘이 포함된 주의 월요일을 기준으로 함
        LocalDate startDate;
        if (weekStart != null && !weekStart.isEmpty()) {
            try {
                startDate = LocalDate.parse(weekStart);
            } catch (Exception e) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "날짜 형식이 올바르지 않습니다.");
            }
        } else {
            // 파라미터가 없으면 오늘이 포함된 주의 월요일을 시작일로 설정
            startDate = LocalDate.now().with(DayOfWeek.MONDAY);
        }

        // 3. 서비스 로직 위임: 통계 데이터 조회 및 추천 목록 생성
        StatReportDto report = selfManagementService.getWeeklyReport(userId, startDate);

        log.info("Weekly report generated for UserId {} starting from {}", userId, startDate);

        return ResponseEntity.ok(report);
    }

}


