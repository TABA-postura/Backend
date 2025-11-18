package com.postura.config;

import com.postura.auth.filter.JwtAuthenticationFilter;
import com.postura.auth.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer; // Customizer 임포트 추가
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 구성을 정의하는 설정 클래스
 * JWT 기반 인증을 사용하며, 세션 관리 및 CSRF 방어 기능을 비활성화합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 비밀번호 암호화를 위한 Encoder Bean 등록
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security 필터 체인 설정 (Spring Security 6.1+ 최신 문법 적용)
     * JWT 인증을 위해 무상태(Stateless) 세션 정책 및 CSRF 비활성화, CORS 설정을 추가합니다.
     * @param http HttpSecurity 설정 객체
     * @return 설정된 SecurityFilterChain
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. JWT 사용을 위해 CSRF 비활성화
                // JWT는 세션에 의존하지 않고 토큰을 통해 인증하므로 CSRF 보호가 필요 없습니다.
                .csrf(csrf -> csrf.disable())

                // 2. CORS 설정 추가 (기본 설정 사용)
                // 프런트엔드 애플리케이션과의 통신을 위해 CORS를 기본값으로 허용합니다.
                .cors(Customizer.withDefaults())

                // 3. JWT는 무상태(Stateless)이므로 세션 사용을 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. HTTP 기본 인증 방식 비활성화 (권장)
                .httpBasic(httpBasic -> httpBasic.disable())

                // 5. form 기반 인증 방식 비활성화 (REST API 환경에서 사용하지 않음)
                .formLogin(formLogin -> formLogin.disable())

                // 6. 요청별 인가(Authorization) 규칙 설정
                .authorizeHttpRequests(authorize -> authorize
                        // 회원가입 및 로그인 API는 인증 없이 누구나 접근 가능하도록 허용
                        .requestMatchers("/api/auth/**", "/api/user/signup").permitAll()
                        // 그 외 모든 요청은 인증된 사용자만 접근 가능
                        .anyRequest().authenticated()
                )

                // 7. JWT 필터를 Spring Security 필터 체인에 등록
                // UsernamePasswordAuthenticationFilter 이전에 JwtAuthenticationFilter를 실행하여 토큰을 검증합니다.
                .addFilterBefore(
                        // JwtTokenProvider를 주입받아 JwtAuthenticationFilter 인스턴스를 생성
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}