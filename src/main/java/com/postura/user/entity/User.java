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
     * Role enum
     */
    @RequiredArgsConstructor
    @Getter
    public enum Role {
        USER("ROLE_USER"),
        ADMIN("ROLE_ADMIN");

        private final String key;
    }

    /**
     * AuthProvider enum
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
     * OAuth2 로그인 시 사용자 프로필 업데이트
     *
     * 정책:
     * - provider는 "자동 전환"하지 않습니다. (LOCAL -> GOOGLE/KAKAO 금지)
     * - 같은 provider인 경우에만 providerId 갱신을 허용합니다.
     * - provider 혼합은 CustomOAuth2UserService에서 차단하는 것이 기본이며,
     *   엔티티에서도 방어적으로 금지합니다.
     */
    public User update(String name, String picture, AuthProvider provider, String providerId) {
        this.name = name;
        this.picture = picture;

        // provider 불일치 방어 (LOCAL <-> SOCIAL, GOOGLE <-> KAKAO 등)
        if (this.provider != null && provider != null && this.provider != provider) {
            throw new IllegalStateException(
                    "Provider mismatch: existing=" + this.provider + ", request=" + provider
            );
        }

        // 같은 provider일 때만 providerId 업데이트(소셜의 경우 보통 갱신/유지)
        if (providerId != null && !providerId.isBlank()) {
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
