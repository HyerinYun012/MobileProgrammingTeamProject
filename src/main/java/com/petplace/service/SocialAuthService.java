package com.petplace.service;

import com.petplace.config.JwtUtil;
import com.petplace.dto.request.SocialLoginRequest; // [추가]
import com.petplace.entity.*;
import com.petplace.exception.BusinessException;
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

    @Transactional
    public String login(SocialLoginRequest req) {
        SocialAuth.Provider provider;
        try {
            provider = SocialAuth.Provider.valueOf(req.getProvider().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("지원하지 않는 소셜 로그인 제공자입니다.");
        }

        // 1. 소셜 서버로부터 검증된 고유 ID(verifiedId) 가져오기
        String verifiedId = getVerifiedId(provider, req.getAccessToken());

        // 2. 이미 해당 소셜 정보로 연동된 계정이 있는지 확인
        return socialAuthRepo.findByProviderAndProviderId(provider, verifiedId)
                .map(sa -> jwtUtil.generateToken(sa.getUser().getId(), sa.getUser().getRole().name()))
                .orElseGet(() -> {
                    // 3. 연동 정보가 없다면 계정 통합 또는 신규 가입 진행
                    return processSocialRegistration(provider, verifiedId, req);
                });
    }

    /**
     * 계정 통합 및 신규 가입 로직
     */
    private String processSocialRegistration(SocialAuth.Provider provider, String verifiedId, SocialLoginRequest req) {
        // [앱 자체 로그인과 공유하는 식별자: 전화번호]
        // 이미 동일한 전화번호로 가입된 '일반 계정'이 있는지 확인합니다.
        User user = userRepo.findByPhone(req.getPhone())
                .orElseGet(() -> {
                    // 기존 계정이 없으면 신규 유저 생성
                    if (userRepo.existsByNickname(req.getNickname())) {
                        throw new BusinessException("이미 사용 중인 닉네임입니다.");
                    }

                    User newUser = new User();
                    newUser.setNickname(req.getNickname());
                    newUser.setPhone(req.getPhone());
                    newUser.setMarketingAgree(req.isMarketingAgree());
                    newUser.setRole(User.Role.from(req.getRole()));
                    return userRepo.save(newUser);
                });

        // 소셜 연동 정보(SocialAuth) 생성 및 연결
        SocialAuth sa = new SocialAuth();
        sa.setUser(user);
        sa.setProvider(provider);
        sa.setProviderId(verifiedId);
        socialAuthRepo.save(sa);

        log.info("소셜 연동 완료: 유저={}, 제공자={}", user.getNickname(), provider);
        return jwtUtil.generateToken(user.getId(), user.getRole().name());
    }

    private String getVerifiedId(SocialAuth.Provider provider, String accessToken) {
        // 1. 공통으로 사용할 변수 선언
        Map<String, Object> info;

        // 2. 제공자에 따라 정보만 가져옴
        if (provider == SocialAuth.Provider.KAKAO) {
            info = getKakaoUserInfo(accessToken);
        } else {
            info = getNaverUserInfo(accessToken);
        }

        // 3. ID 추출 공통 처리 (두 API 모두 최상위 혹은 가공된 Map에 "id"가 있음)
        if (info == null || info.get("id") == null) {
            throw new BusinessException(provider + "로부터 사용자 정보를 불러올 수 없습니다.");
        }

        return String.valueOf(info.get("id"));
    }

    private Map<String, Object> getKakaoUserInfo(String accessToken) {
        return webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorMap(e -> new BusinessException("카카오 인증 실패"))
                .block();
    }

    private Map<String, Object> getNaverUserInfo(String accessToken) {
        Map<String, Object> resp = webClient.get()
                .uri("https://openapi.naver.com/v1/nid/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorMap(e -> new BusinessException("네이버 인증 실패"))
                .block();

        if (resp == null || resp.get("response") == null) throw new BusinessException("네이버 응답 오류");
        return (Map<String, Object>) resp.get("response");
    }
}