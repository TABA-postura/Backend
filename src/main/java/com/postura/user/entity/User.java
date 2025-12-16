package com.postura.user.entity;

import com.postura.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Column(length = 512)
    private String picture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    /**
     * 🔥 Role enum
     */
    @RequiredArgsConstructor
    @Getter
    public enum Role {
        USER("ROLE_USER"),
        ADMIN("ROLE_ADMIN");

        private final String key;
    }

    /**
     * 🔥 AuthProvider enum (LOCAL 추가)
     */
    public enum AuthProvider {
        LOCAL,
        KAKAO,
        GOOGLE
    }

    /**
     * @Builder 생성자
     */
    @Builder
    public User(
            Long id,
            String email,
            String passwordHash,
            String name,
            String picture,
            Role role,
            AuthProvider provider,
            String providerId
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.picture = picture;
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
     * CustomOAuth2UserService에서 호출되는 업데이트 메서드
     * 🔥 수정: 소셜 로그인으로 업데이트 시 Provider 정보를 명시적으로 받아서 업데이트
     */
    public User update(String name, String picture, AuthProvider provider, String providerId) {
        this.name = name;
        this.picture = picture;

        // 🔥 중요: 기존 LOCAL 유저가 소셜 로그인할 경우, Provider와 ProviderId를 업데이트하여 DB 제약 조건을 맞춥니다.
        if (this.provider == AuthProvider.LOCAL) {
            this.provider = provider;
            this.providerId = providerId;
        }

        return this;
    }

    /**
     * 소셜 로그인 유저 생성용 팩토리 메서드
     */
    public static User createSocialUser(
            String email,
            String name,
            String picture,
            AuthProvider provider,
            String providerId
    ) {
        return User.builder()
                .email(email)
                .passwordHash(null)
                .name(name)
                .picture(picture)
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
                .picture(null)
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .providerId(null)
                .build();
    }
}