package com.postura.config;

import com.postura.auth.filter.JwtAuthenticationFilter;
import com.postura.auth.handler.OAuth2AuthenticationSuccessHandler; // 💡 핸들러 임포트
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
    // ✅ OAuth2AuthenticationSuccessHandler 주입 (누락 해결)
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    // TODO: CustomOAuth2UserService를 구현했다면 주입받아야 합니다.
    // private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // 1. CSRF 비활성화 (JWT 기반 Stateless 환경)
                .csrf(csrf -> csrf.disable())

                // 2. CORS 설정 적용
                .cors(Customizer.withDefaults())

                // 3. 세션을 사용하지 않는 Stateless 기반 보안 설정
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4. 인가 규칙 설정
                .authorizeHttpRequests(auth -> auth

                        // CORS Preflight 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 🔥 OAuth2 로그인 시작/콜백 경로 허용 (누락 추가)
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/code/**"
                        ).permitAll()

                        // 🔥 Auth API 및 기타 공개 API (기존 코드 유지)
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/reissue").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/ai/log").permitAll()

                        // 💡 모니터링/리포트 경로는 인증 필요 (기존 코드 유지)
                        .requestMatchers("/monitor/**", "/api/monitor/**").authenticated()
                        .requestMatchers("/report/**","/api/report/**").authenticated()

                        // 🔥 Swagger / API Docs 허용 (기존 코드 유지)
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 🔥 콘텐츠 API는 공개 (기존 코드 유지)
                        .requestMatchers("/api/content/**").permitAll()

                        // 🔥 정적 파일 허용 (기존 코드 유지)
                        .requestMatchers("/videos/**", "/photo/**", "/static/**").permitAll()

                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                )

                // 5. OAuth 2.0 로그인 활성화 (누락된 설정 추가)
                .oauth2Login(oauth2 -> oauth2
                        // TODO: CustomOAuth2UserService를 구현했다면 주석 해제하여 연결
                        // .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))

                        // ✅ 구현한 성공 핸들러를 지정하여 JWT 발급 로직 실행
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )

                // 6. JWT 인증 필터 등록 (기존 코드 유지)
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정 (기존 코드 유지)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // ... (CORS 설정은 기존과 동일)
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:8080",
                "https://d4s7gxwtaejst.cloudfront.net",
                "https://taba-postura.com",
                "http://api.taba-postura.com:8080"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}