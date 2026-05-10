package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // @Builder 사용을 위해 모든 필드를 인자로 받는 생성자 추가
@Builder            // 빌더 패턴 활성화
public class User {

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

    // --- 추가된 필드 ---
    @Builder.Default // 빌더 사용 시 false가 기본값으로 유지되도록 설정
    @Column(nullable = false)
    private boolean isVerified = false; // 사장님 승인 여부
    // ------------------

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ID만 받는 생성자 (기존 코드 유지)
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

    public void updateProfile(String nickname, String profileUrl) {
        this.nickname = nickname;
        this.profileUrl = profileUrl;
    }
}