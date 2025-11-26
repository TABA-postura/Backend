package com.postura.monitor.service;

import com.postura.dto.ai.RealtimeFeedbackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeFeedbackService {

    private final StringRedisTemplate redisTemplate;

    // Redis에 저장할 키의 접두사: posture:feedback:<userId>
    private static final String FEEDBACK_KEY_PREFIX = "posture:feedback:";

    // List 직렬화/역직렬화를 위한 구분자
    private static final String STATE_DELIMITER = ",";

    // 캐시 만료 시간 (예: 10분, 사용자가 오랫동안 모니터링을 중단했을 경우)
    // -> 오래된 데이터를 Redis에서 자동으로 제거하여 메모리 리소스 확보
    private static final long CACHE_EXPIRATION_MINUTES = 10;

    // Redis Hash Field Key 정의
    private static final String FIELD_LATEST_STATES = "states";
    private static final String FIELD_TIMESTAMP = "timestamp";

    // 누적 통계를 위한 필드
    private static final String FIELD_GOOD_COUNT = "good_count";
    private static final String FIELD_WARNING_COUNT = "warning_count";
    private static final String FIELD_TOTAL_COUNT = "total_count";

    /**
     * FastAPI 로그 수신 후, 최신 자세 상태와 누적 통계 카운트를 Redis에 저장/갱신합니다.
     * 이 메서드는 PostureLogService에 의해 비동기로 호출됩니다.
     * @param userId 사용자 ID
     * @param postureStates 현재 감지된 자세 상태 목록
     */
    public void updatePostureCache(Long userId, List<String> postureStates) {
        try {
            String redisKey = FEEDBACK_KEY_PREFIX + userId;

            // 1. 누적 카운트 계산
            long goodCount = postureStates.stream().filter("Good"::equalsIgnoreCase).count();
            // "Good"이 아닌 모든 상태를 경고로 간주 (UNKNOWN 제외)
            long warningCount = postureStates.stream()
                    .filter(s -> !"Good".equalsIgnoreCase(s) && !"UNKNOWN".equalsIgnoreCase(s))
                    .count();

            // 2. 누적 카운트 갱신 (Atomic Increment)
            if (goodCount > 0) {
                redisTemplate.opsForHash().increment(redisKey, FIELD_GOOD_COUNT, goodCount);
            }
            if (warningCount > 0) {
                redisTemplate.opsForHash().increment(redisKey, FIELD_WARNING_COUNT, warningCount);
            }
            // 전체 로그 횟수 (1초에 1회 가정)
            redisTemplate.opsForHash().increment(redisKey, FIELD_TOTAL_COUNT, 1);


            // 3. 최신 상태 정보 저장
            String statesString = String.join(STATE_DELIMITER, postureStates);
            Map<String, String> latestData = new HashMap<>();
            latestData.put(FIELD_LATEST_STATES, statesString);
            latestData.put(FIELD_TIMESTAMP, LocalDateTime.now().toString());

            redisTemplate.opsForHash().putAll(redisKey, latestData);

            // 만료 시간 설정
            redisTemplate.expire(redisKey, CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            // Redis 통신 실패는 핵심 로그 저장(RDS)에 영향을 주지 않도록 처리 (관련 클래스: PostureLogService)
            // -> updatePostureCache가 실패할 경우, 로그 수신 파이프라인 전체가 실패하지 않음
            log.error("Failed to update Redis cache for user {}: {}", userId, e.getMessage());
            // 예외를 다시 던지지 않음 (swallow exception)
        }
    }

    /**
     * 클라이언트의 풀링 요청에 응답하기 위해 Redis에서 최신 데이터를 조회하고 응답 DTO를 생성
     * @param userId 사용자 ID
     * @return RealtimeFeedbackResponse DTO
     */
    public RealtimeFeedbackResponse getRealtimeFeedback(Long userId) {
        String redisKey = FEEDBACK_KEY_PREFIX + userId;

        // 1. Redis에서 Hash 데이터 전체 조회 (Map으로 받는 것이 안전함)
        Map<Object, Object> cachedData = redisTemplate.opsForHash().entries(redisKey);

        // 2. 초기 데이터 없음 처리
        if (cachedData.isEmpty()) {
            return RealtimeFeedbackResponse.builder()
                    .currentPostureStates(Collections.singletonList("UNKNOWN"))
                    .feedbackMessages(Collections.singletonList("모니터링 데이터를 기다리는 중입니다."))
                    .currentTime(LocalDateTime.now().toString())
                    .correctPostureRatio(0.0)
                    .totalWarningCount(0)
                    .build();
        }

        // 3. 데이터 추출
        String statesString = (String) cachedData.getOrDefault(FIELD_LATEST_STATES, "");
        String currentTime = (String) cachedData.getOrDefault(FIELD_TIMESTAMP, LocalDateTime.now().toString());

        // 4. 누적 통계 값 안전하게 파싱
        long goodCount = safeParseLong(cachedData.get(FIELD_GOOD_COUNT));
        long warningCount = safeParseLong(cachedData.get(FIELD_WARNING_COUNT));
        long totalCount = safeParseLong(cachedData.get(FIELD_TOTAL_COUNT));

        // 5. 자세 상태 및 메시지 목록 생성 (기존 로직 유지)
        List<String> postureStates = getPostureStatesList(statesString);
        List<String> feedbackMessages = postureStates.stream()
                .map(this::getSingleFeedbackMessage)
                .collect(Collectors.toList());

        // 6. 유지율 및 경고 횟수 계산
        Integer totalWarningCount = (int) warningCount; // 이미 누적된 경고 횟수 사용
        Double correctPostureRatio = 0.0;

        if (totalCount > 0) {
            // 유지율 계산: (바른 자세 횟수 / 전체 로그 횟수) * 100 -> 소수점 첫째 자리까지 반올림
            correctPostureRatio = Math.round(((double) goodCount / totalCount) * 100.0 * 10.0) / 10.0;
        }

        // 7. DTO 빌드
        return RealtimeFeedbackResponse.builder()
                .currentPostureStates(postureStates)
                .feedbackMessages(feedbackMessages)
                .currentTime(currentTime)
                .correctPostureRatio(correctPostureRatio)
                .totalWarningCount(totalWarningCount)
                .build();
    }

    // *************************************************************
    // 3. 헬퍼 메서드
    // *************************************************************

    /**
     * Redis에서 가져온 Object 값을 안전하게 long 타입으로 파싱합니다.
     */
    private long safeParseLong(Object obj) {
        if (obj == null) return 0L;
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("NumberFormatException during parsing Redis value: {}", obj);
            return 0L;
        }
    }

    /**
     * List<String> 문자열을 복원합니다.
     */
    private List<String> getPostureStatesList(String statesString) {
        if (statesString == null || statesString.isEmpty()) {
            return Collections.singletonList("UNKNOWN");
        }
        return Arrays.stream(statesString.split(STATE_DELIMITER))
                .filter(s -> !s.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 자세 상태에 따라 사용자에게 보낼 코칭 메시지를 생성하는 로직
     * @param postureState FastAPI로부터 수신된 자세 상태 신호 (ex. "FORWARD_HEAD")
     * @return 단일 피드백 메시지
     */
    private String getSingleFeedbackMessage(String postureState) {
        switch (postureState) {
            case "Good":
                return "훌륭합니다! 현재 바른 자세를 유지하고 있습니다. 이 상태를 계속 유지하세요.";

            case "FORWARD_HEAD":
                // 7) 거북목
                return "거북목 감지! 턱 당기기 운동을 하거나 화면과 거리를 두고 목을 뒤로 밀어주세요.";

            case "UNE_SHOULDER":
                // 1) 한쪽 어깨 기울임
                return "한쪽 어깨 기울임 감지! 상부 승모근 스트레칭이나 어깨/가슴 열기 스트레칭이 필요합니다.";

            case "UPPER_TILT":
                // 2) 상체 기울임 (좌/우)
                return "상체 기울임 감지! 의자 등받이에 기대어 몸을 중앙에 맞추고, 측면 몸통 스트레칭을 해주세요.";

            case "TOO_CLOSE":
                // 3) 화면과 너무 가까움
                return "화면 접근 감지! 의자를 뒤로 밀고 가슴 열기 스트레칭을 통해 몸을 이완시켜 주세요.";

            case "ASYMMETRIC":
                // 4) 비대칭 자세 (복합 지표)
                return "복합 비대칭 자세! 척추 회전 스트레칭이나 좌우 어깨 교차 스트레칭으로 균형을 맞춰주세요.";

            case "HEAD_TILT":
                // 5) 머리 기울임
                return "머리 기울임 감지! 목 측면(귀-어깨) 스트레칭을 통해 경부 부담을 완화해 주세요.";

            case "ARM_LEAN":
                // 6) 팔 지지 자세
                return "팔 지지 자세 감지! 손목/전완부 스트레칭 후, 양손을 무릎 위에 올려 바른 자세를 취해주세요.";

            default:
                return "자세 분석 정보를 수신 중입니다. 잠시만 기다려주세요.";
        }
    }
}
