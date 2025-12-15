package com.postura.user.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import java.util.Collection;
import java.util.Map;

// Spring Security가 OAuth2 인증 후 사용하는 사용자 정보 클래스
public class CustomOAuth2User extends DefaultOAuth2User {

    private final String email;
    private final String name; // 여기서는 사용자의 고유 식별자 역할을 합니다 (JWT의 Subject로 사용될 값).

    public CustomOAuth2User(
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String nameAttributeKey,
            String email,
            String name) {

        super(authorities, attributes, nameAttributeKey);
        this.email = email;
        this.name = name; // JWT 발급 시 사용될 사용자 ID
    }

    // JWT 토큰 생성에 필요한 고유 식별자를 반환
    @Override
    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}