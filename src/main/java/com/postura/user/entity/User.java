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
// @AllArgsConstructor 삭제 (수동 Builder 생성자와 충돌 방지)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash; // 이 필드는 DB에서 NULL을 허용하는 것으로 확인됨.

    @Column(nullable = false)
    private String name;

    // 🔥 OAuth2 프로필 사진 URL
    @Column(length = 512)
    private String picture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider; // NOT NULL 제약조건 만족

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

        private final String key; // Spring Security에서 사용하는 권한 키
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
     */
    public User update(String name, String picture) {
        this.name = name;
        this.picture = picture;
        return this;
    }

    /**
     * 소셜 로그인 유저 생성용 팩토리 메서드
     * 🔥 재수정 완료: DB 스키마 확인 결과 passwordHash에 null을 명시합니다.
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
                .passwordHash(null) // ✅ 수정: DB 스키마가 NULL을 허용하므로 null로 설정
                .name(name)
                .picture(picture)
                .role(Role.USER)
                .provider(provider)
                .providerId(providerId)
                .build();
    }

    /**
     * 🔥 로컬 회원가입 유저 생성용 (Provider NOT NULL 오류 해결)
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
                .picture(null) // 로컬 유저는 picture 없음
                .role(Role.USER)
                .provider(AuthProvider.LOCAL) // ✅ provider 필드에 'LOCAL' 값 명시
                .providerId(null) // provider_id는 NULL 허용
                .build();
    }
}