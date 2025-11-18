package com.postura.user.repository;

import com.postura.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User 엔티티를 다루고, User 엔티티의 기본 키 타입은 Long입니다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일(로그인 ID)로 사용자 정보를 조회합니다.
    Optional<User> findByEmail(String email);

    // 주어진 이메일로 사용자가 존재하는지 확인
    boolean existsByEmail(String email);

}
