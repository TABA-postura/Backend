package com.postura.user.entity;

import com.postura.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    /**
     * 이메일
     * - 로컬 로그인 / 소셜 로그인 공통 식별자
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * 비밀번호 해시
     * - LOCAL 로그인만 사용
     * - OAuth 로그인 사용자는 null 허용
     */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /**
     * 사용자 이름
     */
    @Column(nullable = false)
    private String name;

    /**
     * 권한
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * 인증 제공자 (LOCAL, KAKAO, GOOGLE)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    /**
     * OAuth Provider 에서의 고유 ID
     * - LOCAL 로그인은 null
     */
    @Column(name = "provider_id")
    private String providerId;

    public enum Role {
        USER, ADMIN
    }

    public enum AuthProvider {
        LOCAL,
        KAKAO,
        GOOGLE
    }

    @Builder
    public User(
            Long id,
            String email,
            String passwordHash,
            String name,
            Role role,
            AuthProvider provider,
            String providerId
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
    }

    /* =========================
       편의 / 도메인 메서드
       ========================= */

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 소셜 로그인 유저 생성용 팩토리 메서드
     */
    public static User createSocialUser(
            String email,
            String name,
            AuthProvider provider,
            String providerId
    ) {
        return User.builder()
                .email(email)
                .name(name)
                .role(Role.USER)
                .provider(provider)
                .providerId(providerId)
                .build();
    }

    /**
     * 로컬 회원가입 유저 생성용
     */
    public static User createLocalUser(
            String email,
            String passwordHash,
            String name
    ) {
        return User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .name(name)
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .build();
    }
}
