package com.postura.user.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User; // ⭐ 인터페이스만 구현
import lombok.Getter;
import java.util.Collection;
import java.util.Map;

@Getter
// ⭐⭐⭐ DefaultOAuth2User 상속을 제거하고, OAuth2User 인터페이스만 구현합니다. ⭐⭐⭐
public class CustomOAuth2User implements OAuth2User {

    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String email;
    private final String dbIdString; // DB ID (Principal Name으로 사용될 값)

    public CustomOAuth2User(
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String nameAttributeKey, // 이 인자는 CustomOAuth2User 내부에서 무시됩니다.
            String email,
            String dbIdString) {

        this.authorities = authorities;
        this.attributes = attributes;
        this.email = email;
        this.dbIdString = dbIdString; // ⭐ 우리가 원하는 DB ID (문자열)를 저장

        // super(...) 호출이 없어졌으므로, Google ID 주입 문제가 사라집니다.
    }

    // ⭐ 핵심 구현: JWT 발급 시 사용될 DB ID (Principal Name)를 강제적으로 반환합니다.
    @Override
    public String getName() {
        return this.dbIdString;
    }

    // ⭐ 필수 구현: 권한 정보를 반환합니다.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    // ⭐ 필수 구현: Attributes (사용자 정보 Map)를 반환합니다.
    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    // getEmail()은 @Getter가 자동으로 생성합니다.
}