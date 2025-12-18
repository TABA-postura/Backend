package com.postura.user.repository;

import com.postura.user.entity.User;
import com.postura.user.entity.User.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User 엔티티를 다루는 Repository
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회 (LOCAL / OAuth 공통)
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인 (회원가입 중복 체크)
     */
    boolean existsByEmail(String email);

    /**
     * OAuth 로그인용 (provider + providerId)
     */
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
