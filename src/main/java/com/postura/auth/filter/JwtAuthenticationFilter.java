package com.postura.auth.filter;

import com.postura.auth.service.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 공개 엔드포인트는 JWT 필터 자체를 적용하지 않도록 제외
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Preflight 요청은 무조건 제외
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return isPublicEndpoint(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                try {
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT 인증 성공 | user='{}' | path={}", authentication.getName(), request.getRequestURI());

                } catch (JwtException e) {
                    // RefreshToken(auth 없음) 등이 Authorization에 들어온 경우 등
                    SecurityContextHolder.clearContext();
                    log.debug("JWT Authentication 생성 불가(AccessToken 아님/권한 없음) | path={} | msg={}",
                            request.getRequestURI(), e.getMessage());
                }
            } else {
                log.debug("JWT 토큰 없음 또는 검증 실패 | path={}", request.getRequestURI());
            }

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.warn("JWT 필터 처리 중 예외 발생 | path={} | msg={}", request.getRequestURI(), ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 공개 API/리소스 목록
     * - SecurityConfig의 permitAll과 정합을 맞춥니다.
     */
    private boolean isPublicEndpoint(String path) {
        return
                // Auth API
                path.startsWith("/api/auth/") ||

                        // 프로젝트에서 별도로 쓰는 signup 경로 대비
                        path.startsWith("/api/user/signup") ||

                        // OAuth2 시작/콜백
                        path.startsWith("/oauth2/") ||
                        path.startsWith("/login/oauth2/") ||

                        // Spring error
                        path.startsWith("/error") ||

                        // Health
                        path.startsWith("/health") ||

                        // Swagger
                        path.startsWith("/swagger-ui") ||
                        path.startsWith("/swagger-resources") ||
                        path.startsWith("/v3/api-docs") ||

                        // AI 로그(permitAll)
                        path.startsWith("/api/ai/log") ||

                        // Content/Static
                        path.startsWith("/api/content/") ||
                        path.startsWith("/videos/") ||
                        path.startsWith("/photo/") ||
                        path.startsWith("/static/") ||
                        path.startsWith("/images/");
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
