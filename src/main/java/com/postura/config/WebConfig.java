// config/WebConfig.java
package com.postura.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 프론트엔드 환경에 맞게 허용할 Origin 주소를 설정합니다.

        // 프론트엔드 개발 서버 주소 (로컬 React 기본 포트)
        String localFrontend = "http://localhost:3000";

        // 프론트엔드 배포 환경 주소 (CloudFront 또는 S3 도메인)
        String deployedFrontend = "https://d28g9sy3jh6o3a.cloudfront.net";

        // FastAPI 서버 주소는 브라우저를 거치지 않으므로 CORS 설정 불필요

        registry.addMapping("/**") // 모든 API 경로에 대해 CORS 허용
                .allowedOrigins(
                        localFrontend,
                        deployedFrontend
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
                .allowedHeaders("*") // 모든 헤더 허용 (Authorization 헤더 포함)
                .allowCredentials(true) // 자격 증명(JWT, 쿠키 등) 허용
                .maxAge(3600); // 캐시 기간 설정 (3600초)
    }
}