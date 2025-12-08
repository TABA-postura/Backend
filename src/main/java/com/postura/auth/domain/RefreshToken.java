package com.postura.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Refresh Token을 관계형 데이터베이스(RDB)에 저장하는 Entity입니다.
 * JPA를 사용하여 RDB 테이블에 매핑됩니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_token")
public class RefreshToken {

    // Refresh Token을 소유한 사용자의 ID를 기본 키(Primary Key)로 사용합니다.
    // user_id는 불변이며 고유하므로, 엔티티의 식별자로 적합합니다.
    @Id
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId; // <-- @Id를 userId로 변경

    // Refresh Token 문자열은 데이터 필드가 되며, 갱신 가능합니다.
    @Column(name = "token", length = 500, nullable = false)
    private String token; // <-- @Id 제거

    @Builder
    public RefreshToken(Long userId, String token) { // 생성자 인자 순서 변경
        this.userId = userId;
        this.token = token;
    }

    /**
     * Refresh Token 값을 갱신하는 비즈니스 메서드입니다.
     * 이제 ID(userId)는 변경하지 않고, 토큰 값만 새 것으로 변경하므로 Hibernate 오류가 발생하지 않습니다.
     */
    public void updateToken(String newToken) {
        // ID인 userId는 그대로 유지하고 token 값만 새 것으로 변경합니다.
        this.token = newToken;
    }
}