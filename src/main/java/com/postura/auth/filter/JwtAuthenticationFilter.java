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
// 모든 요청에 대해 단 한 번만 실행되도록 보장하는 필터
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * HTTP 요청을 가로채서 JWT를 검증하고 인증 객체를 SecurityContext에 저장합니다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. HTTP 요청 헤더에서 JWT 토큰 추출
        String jwt = resolveToken(request);

        // 2. 추출된 토큰의 유효성 검증
        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {

            // 3. 토큰이 유효하면, 토큰으로부터 Authentication 객체를 얻어옴
            Authentication authentication = jwtTokenProvider.getAuthentication(jwt);

            // 4. SecurityContext에 Authentication 객체를 저장
            // 이로써 해당 요청은 인증된 상태가 됩니다.
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Security Context에 '{}' 인증 정보를 저장했습니다.", authentication.getName());
        }

        // 다음 필터로 요청을 넘김
        filterChain.doFilter(request, response);
    }

    /**
     * Request Header에서 토큰 정보(Bearer XXX)를 추출하는 메서드
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            // "Bearer " 부분을 제외한 실제 토큰 값만 반환
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}