package com.petplace.repository;

import com.petplace.entity.SocialAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SocialAuthRepository extends JpaRepository<SocialAuth, Long> {
    // 단건 조회는 OOM 위험이 없습니다.
    Optional<SocialAuth> findByProviderAndProviderId(SocialAuth.Provider provider, String providerId);

    void deleteByUserId(Long userId);
}