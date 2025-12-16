package com.postura.config;

import com.postura.auth.filter.JwtAuthenticationFilter;
import com.postura.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.postura.auth.service.JwtTokenProvider;
import com.postura.user.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
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
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security Filter Chain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // 1) JWT 기반: CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // 2) Form Login / Basic 비활성화 (API 서버)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // 3) CORS
                .cors(Customizer.withDefaults())

                // 4) 세션 정책
                // - JWT는 Stateless가 기본
                // - OAuth2 Authorization Request 저장을 세션에 의존하는 구성이라면,
                //   별도 Cookie 기반 AuthorizationRequestRepository를 쓰는 방식으로 확장 필요
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 5) 인가 규칙
                .authorizeHttpRequests(auth -> auth
                        // CORS Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Spring 기본 에러 경로
                        .requestMatchers("/error").permitAll()

                        // Health Check (필요 시 actuator로 확장)
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()

                        // 인증/토큰 API (전체 허용)
                        .requestMatchers("/api/auth/**").permitAll()
                        // 프로젝트에서 signup을 /api/user/signup로 쓰는 경우 대비
                        .requestMatchers(HttpMethod.POST, "/api/user/signup").permitAll()

                        // OAuth2 시작/콜백 경로
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        // Swagger / API Docs
                        .requestMatchers("/swagger-ui/**", "/swagger-resources/**", "/v3/api-docs/**").permitAll()

                        // 콘텐츠/정적 리소스
                        .requestMatchers("/api/content/**", "/videos/**", "/photo/**", "/static/**").permitAll()

                        // 모니터/리포트(인증 필요)
                        .requestMatchers("/monitor/**", "/api/monitor/**").authenticated()
                        .requestMatchers("/report/**", "/api/report/**").authenticated()

                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                )

                // 6) OAuth2 로그인
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )

                // 7) 인증 실패 시 401로 통일 (302 리다이렉트 방지)
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                // 8) JWT 인증 필터
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
                "https://www.taba-postura.com",
                "https://api.taba-postura.com",
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
