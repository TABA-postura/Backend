package com.postura.report.controller;

import com.postura.common.exception.CustomException;
import com.postura.common.exception.ErrorCode;
import com.postura.dto.report.StatReportDto;
import com.postura.report.service.SelfManagementService;
import com.postura.user.service.CustomUserDetails;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    // *************************************************************
    // 💡 JWT 인증된 사용자 ID를 SecurityContext에서 추출하는 헬퍼 메서드
    // *************************************************************
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증되지 않았거나 익명 사용자(JWT 검증 실패)인 경우 처리
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();

        // CustomUserDetails 객체에서 userId 추출
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUserId();
        }

        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "인증된 사용자 ID 추출 실패: Principal 타입 불일치");
    }

    /**
     * [GET /report/weekly] 주간 통계 데이터 및 맞춤 추천 조회 엔드포인트
     * userId를 SecurityContext에서 안전하게 획득합니다.
     * @param weekStart 조회할 주의 시작일 (YYYY-MM-DD 형식, 선택적)
     * @return StatReportDto (주간 추이, 요약, 추천 목록)
     */
    @GetMapping("/weekly")
    public ResponseEntity<StatReportDto> getWeeklyReport(
            @RequestParam(required = false) String weekStart)
    {
        // 1. JWT에서 인증된 userId 획득 (쿼리 파라미터 대신 사용)
        Long userId = getAuthenticatedUserId();

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


