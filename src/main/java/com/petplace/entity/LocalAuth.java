package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "local_auth")
@Getter
@Setter
@NoArgsConstructor
// 🌟 중복 필드 제거와 계정 타임스탬프 추적 일관성을 위해 BaseTimeEntity를 상속받습니다.
public class LocalAuth extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(nullable = false, length = 255)
    private String password;
}