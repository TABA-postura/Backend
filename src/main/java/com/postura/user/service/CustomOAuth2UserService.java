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

    /**
     * OAuth2 제공자로부터 받은 사용자 정보를 처리합니다.
     */
    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // Spring Security 기본 서비스로 사용자 정보(attributes)를 가져옴
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 1) 서비스 ID (google, kakao 등)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 2) 사용자 정보의 고유 키 (Google: sub, Kakao: id)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        // 3) 사용자 정보 파싱 및 변환
        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2Attributes oAuth2Attributes =
                OAuth2Attributes.of(registrationId, userNameAttributeName, attributes);

        // 4) DB 저장/업데이트 (provider 충돌 방어 포함)
        User user = saveOrUpdate(oAuth2Attributes);

        log.info("OAuth2 사용자 DB 저장 완료 | provider={} | email={} | userId={}",
                registrationId, user.getEmail(), user.getId());

        // 5) 권한 생성: ROLE_ prefix 보장
        String roleKey = user.getRole() != null ? user.getRole().getKey() : "ROLE_USER";
        if (roleKey != null && !roleKey.startsWith("ROLE_")) {
            roleKey = "ROLE_" + roleKey;
        }
        if (roleKey == null || roleKey.isBlank()) {
            roleKey = "ROLE_USER";
        }

        // 6) CustomOAuth2User 반환
        // - getName()이 DB userId(String)을 반환하도록 유지
        return new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(roleKey)),
                oAuth2Attributes.getAttributes(),
                user.getEmail(),
                user.getId().toString() // ✅ DB Long ID String (JwtTokenProvider가 Long 변환)
        );
    }

    /**
     * DB에 사용자 정보가 있으면 업데이트하고, 없으면 새로 저장합니다.
     *
     * 선택지 A 정책:
     * - 동일 이메일로 provider가 섞이면 로그인 실패(계정 연동 미지원)
     * - 단, 실패는 RuntimeException이 아니라 OAuth2AuthenticationException으로 던져야
     *   Spring Security failureHandler가 프론트 redirect로 처리할 수 있습니다.
     */
    private User saveOrUpdate(OAuth2Attributes attributes) {

        Optional<User> existingOpt = userRepository.findByEmail(attributes.getEmail());

        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();

            // provider 충돌 방어 (LOCAL ↔ GOOGLE/KAKAO, GOOGLE ↔ KAKAO 등)
            if (existing.getProvider() != null && attributes.getProvider() != null
                    && existing.getProvider() != attributes.getProvider()) {

                throw new OAuth2AuthenticationException(
                        new OAuth2Error("provider_mismatch"),
                        "이미 가입된 이메일입니다. 기존 로그인 방식(" + existing.getProvider() + ")으로 로그인해 주세요."
                );
            }

            // 같은 provider면 업데이트
            User updated = existing.update(
                    attributes.getName(),
                    attributes.getPicture(),
                    attributes.getProvider(),
                    attributes.getProviderId()
            );

            User savedUser = userRepository.save(updated);
            log.info("OAuth2 사용자 업데이트 완료 | userId={}", savedUser.getId());
            return savedUser;
        }

        // 신규 사용자면 생성
        User user = attributes.toEntity();
        User savedUser = userRepository.save(user);

        log.info("OAuth2 신규 사용자 저장 완료 | userId={}", savedUser.getId());
        return savedUser;
    }
}
