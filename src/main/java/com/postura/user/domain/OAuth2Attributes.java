package com.postura.user.domain;

import com.postura.user.entity.User;
import com.postura.user.entity.User.AuthProvider;
import lombok.Builder;
import lombok.Getter;
import java.util.Map;

/**
 * OAuth2 Provider별로 다른 사용자 정보(attributes)를 통일시키는 클래스
 */
@Getter
public class OAuth2Attributes {

    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final String name;
    private final String email;
    private final String picture;
    private final AuthProvider provider;

    @Builder
    public OAuth2Attributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email, String picture, AuthProvider provider) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.provider = provider;
    }

    public static OAuth2Attributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equalsIgnoreCase(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuth2Attributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuth2Attributes.builder()
                .name(String.valueOf(attributes.get("name")))
                .email(String.valueOf(attributes.get("email")))
                .picture(String.valueOf(attributes.get("picture")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .provider(AuthProvider.GOOGLE)
                .build();
    }

    private static OAuth2Attributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuth2Attributes.builder()
                .name(String.valueOf(profile.get("nickname")))
                .email(String.valueOf(kakaoAccount.get("email")))
                .picture(String.valueOf(profile.get("profile_image_url")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .provider(AuthProvider.KAKAO)
                .build();
    }

    /**
     * 🔥 수정된 부분: User 엔티티의 팩토리 메서드를 사용하도록 변경
     */
    public User toEntity() {
        // User.createSocialUser 팩토리 메서드를 사용하여 User 엔티티를 생성합니다.
        return User.createSocialUser(
                email,
                name,
                picture, // picture 필드 전달
                provider,
                null // providerId는 CustomOAuth2UserService에서 직접 처리하거나 User 엔티티 생성 시 결정
        );
    }
}