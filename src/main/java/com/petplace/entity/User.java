package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@lombok.experimental.SuperBuilder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(length = 50)
    private String name;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    @Column(length = 500)
    private String profileUrl;

    @Builder.Default
    @Column(nullable = false)
    private boolean marketingAgree = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean isVerified = false; // 사장님 승인 여부

    // ID만 받는 생성자 (기존 영속성 컨텍스트 조회 및 맵핑 편의용 헬퍼 코드 유지)
    public User(Long id) {
        this.id = id;
    }

    public enum Role {
        CUSTOMER, OWNER, ADMIN;

        public static Role from(String roleStr) {
            if (roleStr == null || roleStr.isBlank()) return null;
            try {
                return Role.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /**
     * [도메인 비즈니스 메서드] 마이페이지 프로필 통합 수정
     */
    public void updateProfileInfo(String nickname, String email, String phone, String profileUrl) {
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
        if (profileUrl != null) {
            this.profileUrl = profileUrl;
        }
    }

    /**
     * [도메인 비즈니스 메서드] 사장님 승인 처리
     */
    public void verify() {
        this.isVerified = true;
    }
}