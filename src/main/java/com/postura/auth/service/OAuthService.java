package com.postura.auth.service;

import com.postura.dto.auth.TokenResponse;
import com.postura.user.service.CustomUserDetails;
import com.postura.user.entity.User;
import com.postura.user.entity.User.AuthProvider;
import com.postura.user.repository.UserRepository;
import com.postura.dto.auth.UserInfo; // ✅ UserInfo DTO 사용
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 🔥 최종 수정된 login 메서드: UserInfo DTO를 파라미터로 받습니다.
     */
    @Transactional
    public TokenResponse login(AuthProvider provider, UserInfo userInfo) {

        // 1. 사용자 조회 (email 기반)
        User user = userRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() ->
                        // 2. 사용자가 없으면 새로 생성 (image_18c4ba.png 오류 해결)
                        userRepository.save(
                                User.createSocialUser(
                                        userInfo.getEmail(),
                                        userInfo.getName(),
                                        userInfo.getPicture(), // ✅ picture 파라미터 전달
                                        userInfo.getProvider(),
                                        userInfo.getProviderId()
                                )
                        )
                );

        // 3. 사용자 정보 업데이트
        user.update(userInfo.getName(), userInfo.getPicture());

        // 4. CustomUserDetails 생성
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // 5. Authentication 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                Collections.singletonList(userDetails.getAuthorities().iterator().next())
        );

        // 6. JWT 토큰 생성 및 반환
        return jwtTokenProvider.generateToken(authentication);
    }
}