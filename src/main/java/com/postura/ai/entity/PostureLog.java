package com.postura.ai.entity;

import com.postura.dto.ai.PostureLogRequest;
import com.postura.monitor.entity.MonitoringSession;
import com.postura.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "posture_state", length = 50, nullable = false)
    private String postureState;

    @Lob
    @Column(name = "landmark_data")
    private String landmarkData;

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
                .postureState(request.getPostureStatus())
                .timestamp(request.getTimestamp())
                .landmarkData(request.getLandmarkData())
                .build();
    }
}
