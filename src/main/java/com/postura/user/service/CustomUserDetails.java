package com.postura.user.service;

import com.postura.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Spring Security의 핵심 인터페이스인 **UserDetails**를 구현하여,
 * 애플리케이션의 User 엔티티를 인증 시스템이 사용할 수 있는 형태로 변환하는 Adapter (어댑터) 클래스입니다.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    // **추가: Controller에서 Principal로 userId를 안전하게 추출하기 위한 메서드
    public Long getUserId() {
        // User 엔티티가 getId()를 가진다고 가정
        return user.getId();
    }

    // 사용자 권한을 Spring Security 권한 객체로 변환하여 반환
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" +
                user.getRole().name()));
    }

    // 해시된 비밀번호 반환
    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    // 로그인 ID(이메일) 반환
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    // 계정 상태 관련 메서드는 기본적으로 true 로 설정하여 만료/잠금 사용하지 않음
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
