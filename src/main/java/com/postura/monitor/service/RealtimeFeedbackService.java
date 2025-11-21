package com.postura.monitor.service;

import com.postura.dto.ai.RealtimeFeedbackResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class RealtimeFeedbackService {

    private final StringRedisTemplate redisTemplate;

    // Redis에 저장할 키의 접두사: posture:feedback:<userId>
    private static final String FEEDBACK_KEY_PREFIX = "posture:feedback:";

    // List 직렬화/역직렬화를 위한 구분자
    private static final String STATE_DELIMITER = ",";

    // 캐시 만료 시간 (예: 10분, 사용자가 오랫동안 모니터링을 중단했을 경우)
    // -> 오래된 데이터를 Redis에서 자동으로 제거하여 메모리 리소스 확보
    private static final long CACHE_EXPIRATION_MINUTES = 10;

    /**
     * FastAPI 로그 수신 후, 최신 자세 상태를 Redis Hash 구조에 저장
     * @param userId 사용자 ID
     * @param postureStates 현재 자세 상태
     */
    public void updatePostureCache(Long userId, List<String> postureStates) {
        String redisKey = FEEDBACK_KEY_PREFIX + userId;

        // List<String>을 단일 String으로 변환 (직렬화)
        String statesString = String.join(STATE_DELIMITER, postureStates);

        // Hash 구조에 저장할 데이터 구성
        Map<String, String> data = new HashMap<>();
        data.put("states", statesString);
        data.put("timestamp", LocalDateTime.now().toString());

        // Redis에 데이터 저장 및 만료 시간 설정
        redisTemplate.opsForHash().putAll(redisKey, data);
        redisTemplate.expire(redisKey, CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 클라이언트의 풀링 요청에 응답하기 위해 Redis에서 최신 데이터를 조회하고 응답 DTO를 생성
     * @param userId 사용자 ID
     * @return RealtimeFeedbackResponse DTO
     */
    public RealtimeFeedbackResponse getRealtimeFeedback(Long userId) {
        String redisKey = FEEDBACK_KEY_PREFIX + userId;

        // Redis에서 Hash 데이터 조회
        Map<Object, Object> cachedData = redisTemplate.opsForHash().entries(redisKey);

        if (cachedData.isEmpty() || !cachedData.containsKey("state")) {
            // 데이터가 없거나 유효하지 않은 경우
            return RealtimeFeedbackResponse.builder()
                    .currentPostureStates(Collections.singletonList("UNKNOWN"))
                    .feedbackMessages(Collections.singletonList("모니터링 데이터를 기다리는 중입니다."))
                    .currentTime(LocalDateTime.now().toString())
                    .build();
        }
        String statesString = (String) cachedData.get("states");
        String currentTime = (String) cachedData.getOrDefault("timestamp", LocalDateTime.now().toString());

        // Redis 문자열을 List<String>으로 복원 (역직렬화)
        List<String> postureStates;
        if (statesString == null || statesString.isEmpty()) {
            postureStates = Collections.singletonList("UNKNOWN");
        } else {
            postureStates = Arrays.stream(statesString.split(STATE_DELIMITER))
                    .filter(s -> !s.trim().isEmpty())
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        // 복수 상태에 대한 피드백 메시지 목록 생성
        List<String> feedbackMessages = postureStates.stream()
                .map(this::getSingleFeedbackMessage)
                .collect(Collectors.toList());

        return RealtimeFeedbackResponse.builder()
                .currentPostureStates(postureStates)
                .feedbackMessages(feedbackMessages)
                .currentTime(currentTime)
                .build();
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
