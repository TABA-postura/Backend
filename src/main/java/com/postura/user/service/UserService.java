package com.postura.user.service;

import com.postura.dto.auth.SignUpRequest;
import com.postura.user.entity.User;
import com.postura.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 계정 관련 핵심 비즈니스 로직을 담당
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // SpringSecurity 에 존재

    /**
     * 회원가입 로직 : DTO를 받아 비밀번호를 해시 처리 후 DB에 저장합니다.
     * @param request 회원가입 요청 DTO
     * @return 저장된 User 엔티티
     */
    @Transactional
    public User signUp(SignUpRequest request) {

        // 1. 중복 이메일 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("이미 존재하는 이메일 입니다: " + request.getEmail());
        }

        // 2. 비밀번호 해시(암호화) 처리
        String encodedPasswordHash = passwordEncoder.encode(request.getPassword());

        // 3. User 엔티티 생성
        // 🔥 User.builder() 대신, 로컬 회원가입 전용 팩토리 메서드를 사용합니다.
        // 이 메서드 내부에서 provider 필드에 AuthProvider.LOCAL이 명시적으로 설정됩니다.
        User user = User.createLocalUser(
                request.getEmail(),
                encodedPasswordHash,
                request.getName()
        );

        // 4. DB에 저장 후 반환
        return userRepository.save(user);
    }

    /**
     * 이메일로 사용자 정보를 조회(주로 CustomUserDetailsService에서 사용)
     * @param email 사용자 이메일
     * @return User 엔티티
     */
    @Transactional (readOnly = true)
    public User findUserByEmail (String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
    }
}