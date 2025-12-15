package com.postura.user.entity;

import com.postura.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor; // ✅ RequiredArgsConstructor 임포트 추가

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// @AllArgsConstructor 삭제 (수동 Builder 생성자와 충돌 방지)
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

    // 🔥 필수 추가: OAuth2 프로필 사진 URL
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
     * 🔥 수정된 Role enum: getKey() 메서드 사용 가능하도록 필드와 Lombok 어노테이션 추가
     */
    @RequiredArgsConstructor
    @Getter
    public enum Role {
        USER("ROLE_USER"),
        ADMIN("ROLE_ADMIN");

        private final String key; // Spring Security에서 사용하는 권한 키
    }

    public enum AuthProvider {
        LOCAL,
        KAKAO,
        GOOGLE
    }

    /**
     * @Builder 생성자: picture 필드를 포함하여 재정의
     */
    @Builder
    public User(
            Long id,
            String email,
            String passwordHash,
            String name,
            String picture, // ✅ picture 필드 포함
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
     */
    public User update(String name, String picture) {
        this.name = name;
        this.picture = picture;
        return this;
    }

    /**
     * 소셜 로그인 유저 생성용 팩토리 메서드
     */
    public static User createSocialUser(
            String email,
            String name,
            String picture, // ✅ picture 파라미터 포함
            AuthProvider provider,
            String providerId
    ) {
        return User.builder()
                .email(email)
                .name(name)
                .picture(picture) // ✅ builder 호출 포함
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
                .build();
    }
}