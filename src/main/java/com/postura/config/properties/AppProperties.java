package com.postura.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "app") // application.properties의 'app.'으로 시작하는 설정을 바인딩
public class AppProperties {

    private final Oauth2 oauth2 = new Oauth2();

    @Getter
    @Setter
    public static class Oauth2 {
        /**
         * OAuth2 로그인 성공 후 JWT를 담아 리다이렉트할 프론트엔드 주소 (필수)
         */
        private String authorizedRedirectUri;
    }
}