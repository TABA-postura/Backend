package com.postura.auth.repository;

import com.postura.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
// JpaRepository<[엔티티 타입], [엔티티의 @Id 필드 타입]>
// RefreshToken 엔티티의 @Id가 Long userId로 변경되었으므로, String 대신 Long을 사용합니다.
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Refresh Token 문자열(token)을 기반으로 엔티티를 조회합니다.
    Optional<RefreshToken> findByToken(String token);

    // userId는 이제 엔티티의 Primary Key이므로,
    // findByUserId(Long userId) 대신 JpaRepository의 기본 메서드인 findById(Long userId)를 사용합니다.
    // 명시적인 findByUserId 메서드는 필요에 따라 남겨둘 수 있으나, 여기서는 일반적인 관례에 따라 삭제합니다.
    // 만약 필요하다면 다음과 같이 사용할 수 있습니다:
    // Optional<RefreshToken> findByUserId(Long userId);
}