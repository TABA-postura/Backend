package com.postura.config;

import com.postura.auth.filter.JwtAuthenticationFilter;
import com.postura.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.postura.auth.service.JwtTokenProvider;
import com.postura.user.service.CustomOAuth2UserService;
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
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // 1. CSRF 비활성화 (JWT 기반 Stateless 환경)
                .csrf(csrf -> csrf.disable())

                // 2. Form Login 및 HTTP Basic 명시적 비활성화 (HTML 응답 차단)
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())

                // 3. CORS 설정 적용
                .cors(Customizer.withDefaults())

                // 🔥 3.5. HTTPS 채널 요구 강제 (ALB/CloudFront 환경 필수 설정)
                .requiresChannel(channel -> channel
                        // HTTP 허용이 필요한 특수 경로를 가장 먼저 설정
                        .requestMatchers("/api/ai/**").requiresInsecure()
                        // OAuth2 콜백 경로는 무조건 보안 채널(HTTPS) 요구
                        .requestMatchers("/login/oauth2/code/**").requiresSecure()
                        // 모든 요청을 HTTPS로 강제 (ALB 환경에서 리다이렉트 오류 방지)
                        .anyRequest().requiresSecure()
                )

                // 4. 세션을 사용하지 않는 Stateless 기반 보안 설정
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )


                // 5. 인가 규칙 설정
                .authorizeHttpRequests(auth -> auth
                        // CORS Preflight 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Health Check 경로 허용
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()

                        // Auth API 및 기타 공개 API (permitAll)
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/signup", "/api/auth/reissue", "/api/auth/logout", "/api/ai/log").permitAll()

                        // OAuth2 로그인 시작/콜백 경로 허용
                        .requestMatchers("/oauth2/**", "/login/oauth2/code/**").permitAll()

                        // OAuth2 성공 후 토큰을 전달하는 최종 리다이렉트 URI를 permitAll에 추가
                        .requestMatchers("/oauth/redirect").permitAll()

                        // Swagger / API Docs 허용
                        .requestMatchers("/swagger-ui/**", "/swagger-resources/**", "/v3/api-docs/**").permitAll()

                        // 콘텐츠 API 및 정적 파일 허용
                        .requestMatchers("/api/content/**", "/videos/**", "/photo/**", "/static/**").permitAll()

                        // 모니터링/리포트 경로는 인증 필요
                        .requestMatchers("/monitor/**", "/api/monitor/**").authenticated()
                        .requestMatchers("/report/**","/api/report/**").authenticated()

                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                )

                // 6. OAuth 2.0 로그인 활성화
                .oauth2Login(oauth2 -> oauth2
                        // CustomOAuth2UserService 연결
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        // 구현한 성공 핸들러를 지정하여 JWT 발급 로직 실행
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )

                // 7. 예외 처리: 인증되지 않은 요청에 대해 401 UNAUTHORIZED 반환 강제 (302 리다이렉트 차단)
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                // 8. JWT 인증 필터 등록
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
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