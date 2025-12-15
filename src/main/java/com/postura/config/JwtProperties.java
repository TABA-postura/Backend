package com.postura.config; // 🔥 패키지 위치 수정

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application.properties의 jwt.* 설정 값을 바인딩하는 클래스입니다.
 * ConfigurationProperties를 사용하여 안정적으로 환경 변수와 속성 파일을 로드합니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * application.properties: jwt.secret
     */
    private String secret;

    /**
     * application.properties: jwt.access-token-expiration-in-milliseconds
     */
    private long accessTokenExpirationInMilliseconds;

    /**
     * application.properties: jwt.refresh-token-expiration-in-milliseconds
     */
    private long refreshTokenExpirationInMilliseconds;
}