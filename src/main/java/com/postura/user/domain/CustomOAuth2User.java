package com.postura.user.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import lombok.Getter; // ⭐ Lombok @Getter 임포트
import java.util.Collection;
import java.util.Map;

@Getter // ⭐ Lombok @Getter 추가
// Spring Security가 OAuth2 인증 후 사용하는 사용자 정보 클래스
public class CustomOAuth2User extends DefaultOAuth2User {

    private final String email;
    // ⭐ 필드명을 name 대신 dbIdString으로 변경하여 혼동 방지
    private final String dbIdString;

    public CustomOAuth2User(
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String nameAttributeKey, // 부모 클래스의 초기화를 위해 필요
            String email,
            String dbIdString) { // ⭐ DB ID를 받는 인자

        // 부모 클래스는 여전히 nameAttributeKey ('sub')를 사용해 초기화됩니다. (Google ID가 부모에 저장됨)
        super(authorities, attributes, nameAttributeKey);

        this.email = email;
        this.dbIdString = dbIdString; // ⭐ 우리가 원하는 DB ID (문자열)를 저장
    }

    // ⭐ 핵심 수정: 부모의 동작(Google ID 반환)을 무시하고, 저장된 DB ID만을 강제적으로 반환합니다.
    @Override
    public String getName() {
        return dbIdString;
    }

    // 🚨 Lombok @Getter를 사용하여 getEmail() 메서드를 수동으로 구현할 필요가 없습니다.
    // 하지만 현재 구조를 유지하기 위해 @Getter만 남깁니다.
    // public String getEmail() { return email; }
}