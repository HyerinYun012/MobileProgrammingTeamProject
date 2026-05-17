package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor // JPA 프록시 조회를 위한 기본 생성자 유지
@AllArgsConstructor // 💡 Lombok @SuperBuilder 사용을 위한 필수 구조 보존
// 🌟 부모 클래스(BaseTimeEntity)의 시간 필드를 빌더에서 정상 상속받기 위해 @SuperBuilder로 리팩토링합니다.
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

    @Builder.Default // 빌더 사용 시 Role.CUSTOMER가 무시되지 않도록 설정
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    @Column(length = 500)
    private String profileUrl;

    @Builder.Default // 빌더 사용 시 false가 기본값으로 유지되도록 설정
    @Column(nullable = false)
    private boolean marketingAgree = false;

    @Builder.Default // 빌더 사용 시 false가 기본값으로 유지되도록 설정
    @Column(nullable = false)
    private boolean isVerified = false; // 사장님 승인 여부

    // ID만 받는 생성자 (기존 객체지향 헬퍼 코드 유지)
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

    // 비즈니스 편의 메서드 (Dirty Checking 활용)
    public void updateProfile(String nickname, String profileUrl) {
        this.nickname = nickname;
        this.profileUrl = profileUrl;
        // 프로필 정보가 수정되어 영속성 컨텍스트가 Flush될 때 부모의 updatedAt이 자동으로 함께 갱신됩니다.
    }

    public void verify() {
        this.isVerified = true;
    }
}