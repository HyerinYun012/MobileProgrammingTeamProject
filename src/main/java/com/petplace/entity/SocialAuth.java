package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "social_auth",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"})
)
@Getter
@Setter
@NoArgsConstructor // JPA 프록시 객체 조회를 위한 기본 생성자 유지
// 🌟 중복 필드 제거와 계정 타임스탬프 추적 일관성을 위해 BaseTimeEntity를 상속받습니다.
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
}