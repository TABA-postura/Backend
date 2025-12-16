package com.postura.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor; // ⭐ 추가: @Builder와 함께 사용하여 필드 접근성 보장

/**
 * Refresh Token을 관계형 데이터베이스(RDB)에 저장하는 Entity입니다.
 * JPA를 사용하여 RDB 테이블에 매핑됩니다.
 */
@Entity
@Getter
@Builder // ⭐ 위치 변경: 명시적 생성자 대신 클래스 레벨에 두어 Lombok이 표준 빌더를 생성하도록 합니다.
@AllArgsConstructor // ⭐ 추가: 모든 필드를 인자로 받는 생성자를 생성하여 Builder가 사용할 수 있도록 합니다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_token")
public class RefreshToken {

    // Refresh Token을 소유한 사용자의 ID를 기본 키(Primary Key)로 사용합니다.
    @Id
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // Refresh Token 문자열은 데이터 필드가 되며, 갱신 가능합니다.
    @Column(name = "token", length = 500, nullable = false)
    private String token;

    // 🚨 기존의 명시적 생성자는 @AllArgsConstructor가 대체하므로 삭제하거나 주석 처리합니다.
    /*
    @Builder
    public RefreshToken(Long userId, String token) { // 생성자 인자 순서 변경
        this.userId = userId;
        this.token = token;
    }
    */

    /**
     * Refresh Token 값을 갱신하는 비즈니스 메서드입니다.
     */
    public void updateToken(String newToken) {
        // ID인 userId는 그대로 유지하고 token 값만 새 것으로 변경합니다.
        this.token = newToken;
    }
}