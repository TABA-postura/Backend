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

    // ✅ 추가: providerId 필드 (Google의 sub, Kakao의 id 등 고유 식별자)
    private final String providerId;

    @Builder
    public OAuth2Attributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email, String picture, AuthProvider provider, String providerId) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.provider = provider;
        this.providerId = providerId; // ✅ 추가
    }

    // getProviderId() getter는 @Getter 어노테이션에 의해 자동으로 생성됩니다.

    public static OAuth2Attributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equalsIgnoreCase(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuth2Attributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        // nameAttributeKey에 해당하는 실제 값 (Google의 'sub')을 providerId로 사용
        String providerId = String.valueOf(attributes.get(userNameAttributeName));

        return OAuth2Attributes.builder()
                .name(String.valueOf(attributes.get("name")))
                .email(String.valueOf(attributes.get("email")))
                .picture(String.valueOf(attributes.get("picture")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .provider(AuthProvider.GOOGLE)
                .providerId(providerId) // ✅ 추가
                .build();
    }

    private static OAuth2Attributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        // nameAttributeKey에 해당하는 실제 값 (Kakao의 'id')을 providerId로 사용
        String providerId = String.valueOf(attributes.get(userNameAttributeName));

        return OAuth2Attributes.builder()
                .name(String.valueOf(profile.get("nickname")))
                .email(String.valueOf(kakaoAccount.get("email")))
                .picture(String.valueOf(profile.get("profile_image_url")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .provider(AuthProvider.KAKAO)
                .providerId(providerId) // ✅ 추가
                .build();
    }

    /**
     * User 엔티티의 팩토리 메서드를 사용하도록 변경 및 providerId 전달
     */
    public User toEntity() {

        // toEntity 로직은 이미 providerId 필드를 가지고 있으므로 로직은 단순화됩니다.
        return User.createSocialUser(
                email,
                name,
                picture,
                provider,
                this.providerId // ✅ 필드에 저장된 providerId 사용
        );
    }
}