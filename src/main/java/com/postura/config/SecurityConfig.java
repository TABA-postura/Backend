package com.postura.config;

import com.postura.auth.filter.JwtAuthenticationFilter;
import com.postura.auth.handler.OAuth2AuthenticationFailureHandler;
import com.postura.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.postura.auth.service.JwtTokenProvider;
import com.postura.user.service.CustomOAuth2UserService;
import com.postura.user.service.CustomOidcUserService;
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
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    private final CustomOAuth2UserService customOAuth2UserService; // kakao 등 OAuth2
    private final CustomOidcUserService customOidcUserService;     // google 등 OIDC

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .cors(Customizer.withDefaults())

                // OAuth2 플로우 안정성을 위해 IF_REQUIRED 유지 권장
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()

                        // Auth API
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/user/signup").permitAll()

                        // OAuth2 시작/콜백
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        // Swagger
                        .requestMatchers("/swagger-ui/**", "/swagger-resources/**", "/v3/api-docs/**").permitAll()

                        // 콘텐츠/정적
                        .requestMatchers("/api/content/**", "/videos/**", "/photo/**", "/static/**", "/images/**").permitAll()

                        // AI 로그
                        .requestMatchers(HttpMethod.POST, "/api/ai/log").permitAll()

                        // 보호 API
                        .requestMatchers("/monitor/**", "/api/monitor/**").authenticated()
                        .requestMatchers("/report/**", "/api/report/**").authenticated()

                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )

                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정 (운영 도메인 최소화)
     *
     * 주의:
     * - allowCredentials(true) 사용 시 allowedOrigins에 "*" 사용 불가
     * - 프론트 Origin만 허용하면 됨 (API 자신의 도메인은 보통 필요 없음)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ 포트가 바뀌는 로컬 환경까지 커버하려면 patterns가 가장 편합니다.
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://taba-postura.com",
                "https://www.taba-postura.com",
                "https://d4s7gxwtaejst.cloudfront.net"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Authorization, Content-Type 등 전부 허용(필요하면 나중에 최소화 가능)
        config.setAllowedHeaders(List.of("*"));

        // 프론트에서 응답 헤더를 읽어야 할 때
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));

        config.setAllowCredentials(true);

        // ✅ Preflight 캐시(브라우저)로 OPTIONS 비용 감소
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
