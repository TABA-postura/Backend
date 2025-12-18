package com.postura.user.service;

import com.postura.user.domain.CustomOAuth2User;
import com.postura.user.domain.OAuth2Attributes;
import com.postura.user.entity.User;
import com.postura.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2Attributes oAuth2Attributes =
                OAuth2Attributes.of(registrationId, userNameAttributeName, attributes);

        User user = saveOrUpdate(oAuth2Attributes);

        String roleKey = user.getRole() != null ? user.getRole().getKey() : "ROLE_USER";
        if (roleKey != null && !roleKey.startsWith("ROLE_")) roleKey = "ROLE_" + roleKey;
        if (roleKey == null || roleKey.isBlank()) roleKey = "ROLE_USER";

        return new CustomOAuth2User(
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

            // ✅ provider mismatch는 OAuth2AuthenticationException으로 던져야 failureHandler가 잡고 프론트로 리다이렉트됨
            if (existing.getProvider() != null && attributes.getProvider() != null
                    && existing.getProvider() != attributes.getProvider()) {

                throw new OAuth2AuthenticationException(
                        new OAuth2Error("provider_mismatch"),
                        "이미 가입된 이메일입니다. 기존 로그인 방식(" + existing.getProvider() + ")으로 로그인해 주세요."
                );
            }

            User updated = existing.update(
                    attributes.getName(),
                    attributes.getPicture(),
                    attributes.getProvider(),
                    attributes.getProviderId()
            );

            User saved = userRepository.save(updated);
            log.info("OAuth2 사용자 업데이트 완료 | userId={}", saved.getId());
            return saved;
        }

        User saved = userRepository.save(attributes.toEntity());
        log.info("OAuth2 신규 사용자 저장 완료 | userId={}", saved.getId());
        return saved;
    }
}
