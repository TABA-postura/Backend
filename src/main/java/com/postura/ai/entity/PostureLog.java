package com.postura.ai.entity;

import com.postura.common.util.StringListConverter;
import com.postura.dto.ai.PostureLogRequest;
import com.postura.monitor.entity.MonitoringSession;
import com.postura.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Builder // DTO를 Entity로 변환 시 사용
@NoArgsConstructor(access =AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "posture_log")
public class PostureLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private MonitoringSession session;

    // report 모듈에서 통계를 정확하게 집계하는 유일한 시간 기준
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Convert(converter = StringListConverter.class)
    @Column(name = "posture_states", columnDefinition = "TEXT", nullable = false)
    private List<String> postureStates;

    /**
     * DTO에서 Entity로 변환 (Service 계층에서 사용)
     * DTO에는 User와 Session 객체 없음 -> ID를 통해서 받아와야 함
     */
    public static PostureLog from (
            PostureLogRequest request,
            User user,
            MonitoringSession session
    ){
        return PostureLog.builder()
                .user(user)
                .session(session)
                .postureStates(request.getPostureStates())
                .timestamp(request.getTimestamp())
                .build();
    }
}
