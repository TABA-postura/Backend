package com.postura.auth.filter;

import com.postura.auth.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 🔹 Preflight 요청은 바로 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 🔹 인증이 필요 없는 엔드포인트는 JWT 검사 없이 통과
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 🔹 Authorization 헤더에서 JWT 추출
            String token = resolveToken(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("🔐 JWT 인증 성공 — user='{}'", authentication.getName());
            } else {
                // 토큰이 없거나 유효하지 않더라도 예외를 발생시키지 않고 필터 체인을 진행 (다음 필터에게 인가를 맡김)
                log.debug("❌ JWT 토큰 없음 또는 검증 실패 — path={}", path);
            }

        } catch (Exception ex) {
            log.error("JWT 인증 중 오류 발생: {}", ex.getMessage());

            // 🔥 수정: 강제 401 응답 로직을 제거했습니다!
            // OAuth2 성공 응답이 이 로직 때문에 막혔습니다.
            // 인증 실패 시의 최종 401 처리는 SecurityConfig의 exceptionHandling에 맡깁니다.
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 인증이 필요 없는 공개 API 목록 (OAuth2 콜백 경로 추가)
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/signup") ||
                path.startsWith("/api/auth/reissue") ||
                path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/login/oauth2/code"); // ✅ 수정: OAuth2 콜백 경로 추가
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) &&
                bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}