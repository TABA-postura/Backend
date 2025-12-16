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
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

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

        // 1. 서비스 ID (google, kakao 등) 추출
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 2. 사용자 정보의 고유 키 (Primary Key) 추출 (Google은 'sub', Kakao는 'id')
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 3. 사용자 정보 파싱 및 변환
        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2Attributes oAuth2Attributes = OAuth2Attributes.of(registrationId, userNameAttributeName, attributes);

        // 4. DB에 사용자 저장/업데이트
        User user = saveOrUpdate(oAuth2Attributes);

        // 🔥🔥 최종 확인 로그: DB 저장 성공 여부를 확인하는 결정적인 로그
        log.info("✅ DB 저장 완료: Provider={} | Email={} | UserID={}",
                registrationId, user.getEmail(), user.getId());

        // 5. Spring Security CustomOAuth2User 객체 생성 및 반환
        return new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getKey())),
                oAuth2Attributes.getAttributes(),
                oAuth2Attributes.getNameAttributeKey(),
                user.getEmail(), // CustomOAuth2User의 email
                user.getId().toString() // CustomOAuth2User의 name (JWT Subject로 사용될 고유 ID)
        );
    }

    /**
     * DB에 사용자 정보가 있으면 업데이트하고, 없으면 새로 저장합니다.
     */
    private User saveOrUpdate(OAuth2Attributes attributes) {

        // 이메일을 기반으로 사용자 조회
        User user = userRepository.findByEmail(attributes.getEmail())
                // ✅ 수정: 기존 사용자면 update 메서드에 provider 및 providerId를 추가로 전달
                .map(entity -> entity.update(attributes.getName(),
                        attributes.getPicture(),
                        attributes.getProvider(),      // AuthProvider 전달
                        attributes.getProviderId()))   // ProviderId 전달
                .orElse(attributes.toEntity()); // 새 사용자면 엔티티 생성

        return userRepository.save(user); // DB에 저장/업데이트
    }
}