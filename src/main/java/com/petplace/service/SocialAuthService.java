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
@Transactional
public class SocialAuthService {
    private final UserRepository userRepo;
    private final SocialAuthRepository socialAuthRepo;
    private final JwtUtil jwtUtil;
    private final WebClient webClient = WebClient.create();

    /**
     * 통합 소셜 로그인 처리 (DTO 기반)
     */
    public String login(SocialLoginRequest req) {
        SocialAuth.Provider provider;
        try {
            provider = SocialAuth.Provider.valueOf(req.getProvider().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("지원하지 않는 소셜 로그인 제공자입니다.");
        }

        // 1. 이미 가입된 연동 정보가 있는지 확인
        return socialAuthRepo.findByProviderAndProviderId(provider, req.getProviderId())
                .map(sa -> jwtUtil.generateToken(sa.getUser().getId(), sa.getUser().getRole().name()))
                .orElseGet(() -> {
                    // 2. 가입되지 않은 경우 신규 회원가입 진행
                    return registerNewSocialUser(provider, req);
                });
    }

    private String registerNewSocialUser(SocialAuth.Provider provider, SocialLoginRequest req) {
        // 닉네임 중복 체크
        if (userRepo.existsByNickname(req.getNickname())) {
            throw new BusinessException("이미 사용 중인 닉네임입니다.");
        }

        User u = new User();
        u.setNickname(req.getNickname());
        u.setPhone(req.getPhone());
        u.setMarketingAgree(req.isMarketingAgree());

        // 안전한 변환 메서드 호출
        User.Role role = User.Role.from(req.getRole());

        // 변환 실패 시 비즈니스 예외로 통합 처리
        if (role == null) {
            throw new BusinessException("잘못된 사용자 권한 요청입니다. (입력값: " + req.getRole() + ")");
        }

        u.setRole(role);
        userRepo.save(u);

        SocialAuth sa = new SocialAuth();
        sa.setUser(u);
        sa.setProvider(provider);
        sa.setProviderId(req.getProviderId());
        socialAuthRepo.save(sa);

        return jwtUtil.generateToken(u.getId(), u.getRole().name());
    }

    // --- 레거시 호환 및 외부 API 직접 검증이 필요한 경우 사용 ---

    public Map<String, Object> getKakaoUserInfo(String accessToken) {
        return webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorMap(e -> new BusinessException("카카오 인증 서버와 통신 중 오류가 발생했습니다."))
                .block();
    }

    public Map<String, Object> getNaverUserInfo(String accessToken) {
        Map<String, Object> resp = webClient.get()
                .uri("https://openapi.naver.com/v1/nid/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorMap(e -> new BusinessException("네이버 인증 서버와 통신 중 오류가 발생했습니다."))
                .block();

        return (Map<String, Object>) resp.get("response");
    }
}