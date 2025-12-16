package com.postura.user.domain;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

@Getter
public class CustomOAuth2User implements OAuth2User, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String email;

    /**
     * DB PK(Long)의 String 표현.
     * - OAuth2 성공 후 principal.getName()으로 userId를 얻기 위해 사용
     */
    private final String dbIdString;

    public CustomOAuth2User(
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String email,
            String dbIdString
    ) {
        this.authorities = Objects.requireNonNull(authorities, "authorities must not be null");
        this.attributes = Objects.requireNonNull(attributes, "attributes must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.dbIdString = Objects.requireNonNull(dbIdString, "dbIdString must not be null");
    }

    /**
     * principal.getName()이 DB userId(String)을 반환하도록 강제
     */
    @Override
    public String getName() {
        return this.dbIdString;
    }
}
