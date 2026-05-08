package com.petplace.repository;
import com.petplace.entity.SocialAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SocialAuthRepository extends JpaRepository<SocialAuth, Long> {
    Optional<SocialAuth> findByProviderAndProviderId(SocialAuth.Provider provider, String providerId);
}
