package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "social_auth",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 🛡️ JPA 프록시용 공간은 남기되, 외부에서 new SocialAuth()하는 행위 차단
public class SocialAuth extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(nullable = false, length = 100)
    private String providerId;

    public enum Provider { KAKAO, NAVER }

    /**
     * 💡 [정적 팩토리 메서드] 소셜 계정 연동 창구 일원화
     * 소셜 정보는 최초 연동(회원가입) 시점에 유저, 플랫폼, 고유 ID 쌍이 완벽하게 고정되어야 합니다.
     * 수정(Update) 비즈니스는 존재하지 않으며, 연동 해제 시에는 엔티티를 완전 삭제(Delete) 처리합니다.
     */
    public static SocialAuth createSocialAuth(User user, Provider provider, String providerId) {
        SocialAuth socialAuth = new SocialAuth();
        socialAuth.user = user;
        socialAuth.provider = provider;
        socialAuth.providerId = providerId;
        return socialAuth;
    }
}