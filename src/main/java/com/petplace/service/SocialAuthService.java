package com.petplace.service;

import com.petplace.config.JwtUtil;
import com.petplace.dto.request.SocialLoginRequest;
import com.petplace.entity.*;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final UserRepository userRepo;
    private final SocialAuthRepository socialAuthRepo;
    private final JwtUtil jwtUtil;
    private final WebClient webClient = WebClient.create();

    @Transactional
    public String login(SocialLoginRequest req) {
        SocialAuth.Provider provider;
        try {
            provider = SocialAuth.Provider.valueOf(req.getProvider().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
        }

        String verifiedId = getVerifiedId(provider, req.getAccessToken());

        return socialAuthRepo.findByProviderAndProviderId(provider, verifiedId)
                .map(sa -> jwtUtil.generateToken(sa.getUser().getId(), sa.getUser().getRole().name()))
                .orElseGet(() -> {
                    // 닉네임 생성 로직
                    String randomNickname = provider.name() + "_" + UUID.randomUUID().toString().substring(0, 8);
                    while (userRepo.existsByNickname(randomNickname)) {
                        randomNickname = provider.name() + "_" + UUID.randomUUID().toString().substring(0, 8);
                    }

                    // 🚨 [보안 수정] 클라이언트의 요청(req)을 절대 신뢰하지 않고 서버 측에서 Role을 고정합니다.
                    // Mass Assignment 공격을 원천 차단하기 위해 User.Role.CUSTOMER로 하드코딩합니다.
                    User newUser = User.builder()
                            .nickname(randomNickname)
                            .role(User.Role.CUSTOMER)
                            .build();
                    userRepo.save(newUser);

                    SocialAuth socialAuth = SocialAuth.createSocialAuth(newUser, provider, verifiedId);
                    socialAuthRepo.save(socialAuth);

                    return jwtUtil.generateToken(newUser.getId(), newUser.getRole().name());
                });
    }

    private String getVerifiedId(SocialAuth.Provider provider, String accessToken) {
        Map<String, Object> info;
        if (provider == SocialAuth.Provider.KAKAO) {
            info = getKakaoUserInfo(accessToken);
        } else {
            info = getNaverUserInfo(accessToken);
        }

        if (info == null || info.get("id") == null) {
            throw new BusinessException(ErrorCode.SOCIAL_INFO_FETCH_FAILED);
        }

        return String.valueOf(info.get("id"));
    }

    private Map<String, Object> getKakaoUserInfo(String accessToken) {
        return webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorMap(e -> new BusinessException(ErrorCode.KAKAO_AUTH_FAILED))
                .block();
    }

    private Map<String, Object> getNaverUserInfo(String accessToken) {
        Map<String, Object> resp = webClient.get()
                .uri("https://openapi.naver.com/v1/nid/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorMap(e -> new BusinessException(ErrorCode.NAVER_AUTH_FAILED))
                .block();

        if (resp == null || !"00".equals(resp.get("resultcode"))) {
            throw new BusinessException(ErrorCode.NAVER_INFO_FETCH_FAILED);
        }
        return (Map<String, Object>) resp.get("response");
    }
}