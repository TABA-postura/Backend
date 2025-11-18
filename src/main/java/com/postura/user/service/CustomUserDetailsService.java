package com.postura.user.service;

import com.postura.user.entity.User;
import com.postura.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailService 인터페이스(Spring Security 프레임워크에 존재)를 구현하여 DB에서 사용자 정보를
 * 로드하는 핵심적인 역할 수행
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * SpringSecurity의 인증 과정에서 사용자 ID(여기서는 이메일)을 기반으로
     * DB에서 사용자 정보를 로드합니다.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        // 찾은 User 엔티티를 CustomUserDetail 로 래핑하여 반환
        return new CustomUserDetails(user);
    }


}
