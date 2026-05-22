package com.petplace.service;

import com.petplace.config.JwtUtil;
import com.petplace.dto.request.SocialLoginRequest;
import com.petplace.dto.request.SocialSignupRequest;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final UserRepository userRepo;
    private final SocialAuthRepository socialAuthRepo;
    private final JwtUtil jwtUtil;
    private final WebClient webClient = WebClient.create();

    /**
     * 1. [순수 소셜 로그인 API 로직]
     * 연동된 계정이 있으면 JWT 토큰 발급, 없으면 신규 유저 예외(401)를 발생시켜 가입 유도
     */
    @Transactional(readOnly = true)
    public String login(SocialLoginRequest req) {
        SocialAuth.Provider provider = convertProvider(req.getProvider());
        String verifiedId = getVerifiedId(provider, req.getAccessToken());

        // 소셜 연동 테이블 조회 후 존재하지 않으면 신규 회원 예외 발생
        SocialAuth socialAuth = socialAuthRepo.findByProviderAndProviderId(provider, verifiedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NEW_SOCIAL_USER_NEED_SIGNUP));

        return jwtUtil.generateToken(socialAuth.getUser().getId(), socialAuth.getUser().getRole().name());
    }

    /**
     * 2. [추가 정보 회원가입 API 로직]
     * 신규 소셜 유저가 프론트엔드 화면에서 작성한 진짜 데이터를 받아 DB에 최종 저장
     */
    @Transactional
    public String signup(SocialSignupRequest req) {
        SocialAuth.Provider provider = convertProvider(req.getProvider());
        String verifiedId = getVerifiedId(provider, req.getAccessToken());

        // [안전장치] 동일한 소셜 계정으로 동시/중복 가입 시도 차단
        if (socialAuthRepo.findByProviderAndProviderId(provider, verifiedId).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_SOCIAL_USER); // 적절한 중복 가입 에러 정의 필요
        }

        // 사용자가 입력한 닉네임 중복 체크
        if (userRepo.existsByNickname(req.getNickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // 🌟 1. 유저 정보 생성 (프론트엔드에서 폼 양식으로 전송해준 진짜 정보 활용)
        User newUser = User.createSocialUser(
                req.getNickname(),
                req.getPhone(),
                User.Role.from(req.getRole()),
                req.isMarketingAgree()
        );
        userRepo.save(newUser);

        // 🌟 2. 소셜 연동 매핑 정보 생성 및 저장
        SocialAuth socialAuth = SocialAuth.createSocialAuth(newUser, provider, verifiedId);
        socialAuthRepo.save(socialAuth);

        // 🌟 3. 가입 성공 후 즉시 로그인이 되도록 JWT 토큰 발급
        return jwtUtil.generateToken(newUser.getId(), newUser.getRole().name());
    }

    /**
     * 프로바이더 영문 문자열 검증 및 Enum 변환 공통 헬퍼 메서드
     */
    private SocialAuth.Provider convertProvider(String providerStr) {
        try {
            return SocialAuth.Provider.valueOf(providerStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
        }
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