package com.postura.user.entity;

import com.postura.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalTime;

/**
 * 사용자 정보를 데이터베이스에 매핑하는 User 엔티티 클래스입니다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 외부에서 객체 성성 방지
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 키 자동 생성, 자동 증가
    @Column(name = "user_id")
    private Long id; // 자바 필드명은 관례상 id

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash; // 해시된 비밀번호

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // 권한 정보

    public enum Role {
        USER, ADMIN
    }

    /**
     *@Builder 패턴은 필드에 값을 명확하게 지정하면서
     * 객체를 생성하도록 유도하여 가독성이 높고 안전한(불변성에 가까운)
     * 객체 생성을 가능하게 합니다.
     */
    @Builder
    public User(String email, String passwordHash, String name, Role role, LocalTime startTime, LocalTime endtime) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
    }

    // 편의 메서드 : 비밀번호 해시 업데이트 등
    public void updatePasswordHash(String passwordHash){
        this.passwordHash = passwordHash;
    }
}
