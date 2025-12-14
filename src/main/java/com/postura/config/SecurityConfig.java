package com.postura.config;

import com.postura.auth.filter.JwtAuthenticationFilter;
import com.postura.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.postura.auth.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    // OAuth2 로그인 성공 후 처리를 위한 핸들러 주입
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // JWT 기반 인증에서는 CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // CORS 설정 적용
                .cors(Customizer.withDefaults())

                // Stateless 세션 (JWT와 OAuth2 모두)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 인가 규칙
                .authorizeHttpRequests(auth -> auth

                        // Preflight 요청 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Auth API 전체 공개 (LOCAL + OAUTH 리다이렉션 경로 포함)
                        .requestMatchers("/api/auth/**", "/oauth2/**", "/login/oauth2/code/**").permitAll()

                        // AI 로그 공개
                        .requestMatchers(HttpMethod.POST, "/api/ai/log").permitAll()

                        // 인증 필요한 API
                        .requestMatchers("/monitor/**", "/api/monitor/**").authenticated()
                        .requestMatchers("/report/**", "/api/report/**").authenticated()

                        // Swagger 허용
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 콘텐츠 공개
                        .requestMatchers("/api/content/**").permitAll()

                        // 정적 리소스 허용
                        .requestMatchers("/videos/**", "/photo/**", "/static/**").permitAll()

                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                )

                // 🚨 OAuth 2.0 로그인 활성화 (오류 수정 부분)
                .oauth2Login(oauth2 -> oauth2
                                // 인증 성공 후, OAuth2AuthenticationSuccessHandler 호출
                                .successHandler(oAuth2AuthenticationSuccessHandler)
                        // Spring Security가 사용자 정보 획득을 자동 처리하는 것을 막기 위해
                        // .userInfoEndpoint() 체인 자체를 삭제합니다.
                )

                // JWT 필터
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * CORS 설정 (최종 도메인 추가)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:8080",
                "https://d28g9sy3jh6o3a.cloudfront.net",
                "https://taba-postura.com"
        ));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(
                "Authorization", "Content-Type"
        ));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}