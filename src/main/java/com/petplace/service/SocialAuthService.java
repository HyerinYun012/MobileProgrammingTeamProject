package com.petplace.service;
import com.petplace.config.JwtUtil;
import com.petplace.entity.*;
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;
@Service @RequiredArgsConstructor @Transactional
public class SocialAuthService {
    private final UserRepository userRepo;
    private final SocialAuthRepository socialAuthRepo;
    private final JwtUtil jwtUtil;
    private final WebClient webClient = WebClient.create();

    public String kakaoLogin(String accessToken, String role, String nickname, String phone, boolean marketing) {
        Map<String, Object> kakaoUser = webClient.get()
            .uri("https://kapi.kakao.com/v2/user/me")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve().bodyToMono(Map.class).block();
        String providerId = String.valueOf(kakaoUser.get("id"));
        return process(SocialAuth.Provider.KAKAO, providerId, nickname, phone, marketing, role);
    }

    public String naverLogin(String accessToken, String role, String nickname, String phone, boolean marketing) {
        Map<String, Object> resp = webClient.get()
            .uri("https://openapi.naver.com/v1/nid/me")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve().bodyToMono(Map.class).block();
        Map<String, Object> naverUser = (Map<String, Object>) resp.get("response");
        String providerId = (String) naverUser.get("id");
        return process(SocialAuth.Provider.NAVER, providerId, nickname, phone, marketing, role);
    }

    private String process(SocialAuth.Provider provider, String providerId, String nickname, String phone, boolean marketing, String role) {
        return socialAuthRepo.findByProviderAndProviderId(provider, providerId)
            .map(sa -> jwtUtil.generateToken(sa.getUser().getId(), sa.getUser().getRole().name()))
            .orElseGet(() -> {
                User u = new User(); u.setNickname(nickname); u.setPhone(phone);
                u.setMarketingAgree(marketing); u.setRole(User.Role.valueOf(role)); userRepo.save(u);
                SocialAuth sa = new SocialAuth(); sa.setUser(u); sa.setProvider(provider);
                sa.setProviderId(providerId); socialAuthRepo.save(sa);
                return jwtUtil.generateToken(u.getId(), u.getRole().name());
            });
    }
}
