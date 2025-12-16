package com.postura.user.domain;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

@Getter
public class CustomOidcUser implements OidcUser, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final OidcUser delegate;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;

    private final String email;
    private final String dbIdString; // principal.getName()으로 반환할 DB PK 문자열

    public CustomOidcUser(OidcUser delegate,
                          Collection<? extends GrantedAuthority> authorities,
                          Map<String, Object> attributes,
                          String email,
                          String dbIdString) {
        this.delegate = delegate;
        this.authorities = authorities;
        this.attributes = attributes;
        this.email = email;
        this.dbIdString = dbIdString;
    }

    @Override
    public String getName() {
        // ✅ 핵심: DefaultOidcUser가 아니라 DB userId를 name으로 강제
        return this.dbIdString;
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }
}
