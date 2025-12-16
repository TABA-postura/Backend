package com.postura.user.service;

import com.postura.user.domain.CustomOidcUser;
import com.postura.user.domain.OAuth2Attributes;
import com.postura.user.entity.User;
import com.postura.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final UserRepository userRepository;
    private final OidcUserService delegate = new OidcUserService();

    @Transactional
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        // 1) 기본 OIDC 유저 로드 (principal=DefaultOidcUser 생성되는 지점)
        OidcUser oidcUser = delegate.loadUser(userRequest);

        // 2) provider 식별자
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 3) userNameAttributeKey (보통 google=sub)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        // 4) attributes -> OAuth2Attributes 변환 (기존 로직 재사용)
        Map<String, Object> attributes = oidcUser.getAttributes();
        OAuth2Attributes oAuth2Attributes =
                OAuth2Attributes.of(registrationId, userNameAttributeName, attributes);

        // 5) DB 저장/업데이트 (provider 충돌 방어 포함)
        User user = saveOrUpdate(oAuth2Attributes);

        // 6) 권한 생성 (ROLE_ prefix 보장)
        String roleKey = user.getRole() != null ? user.getRole().getKey() : "ROLE_USER";
        if (roleKey != null && !roleKey.startsWith("ROLE_")) roleKey = "ROLE_" + roleKey;
        if (roleKey == null || roleKey.isBlank()) roleKey = "ROLE_USER";

        // 7) ✅ CustomOidcUser로 감싸서 getName() = DB userId(String) 보장
        return new CustomOidcUser(
                oidcUser,
                Collections.singleton(new SimpleGrantedAuthority(roleKey)),
                oAuth2Attributes.getAttributes(),
                user.getEmail(),
                user.getId().toString()
        );
    }

    private User saveOrUpdate(OAuth2Attributes attributes) {
        Optional<User> existingOpt = userRepository.findByEmail(attributes.getEmail());

        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();

            if (existing.getProvider() != null && attributes.getProvider() != null
                    && existing.getProvider() != attributes.getProvider()) {
                throw new RuntimeException(
                        "이미 가입된 이메일입니다. 기존 로그인 방식(" + existing.getProvider() + ")으로 로그인해 주세요."
                );
            }

            User updated = existing.update(
                    attributes.getName(),
                    attributes.getPicture(),
                    attributes.getProvider(),
                    attributes.getProviderId()
            );

            return userRepository.save(updated);
        }

        return userRepository.save(attributes.toEntity());
    }
}
