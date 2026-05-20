package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "local_auth")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 🛡️ 무분별한 빈 객체 생성 차단 및 JPA 스펙 준수
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

    /**
     * 💡 [정적 팩토리 메서드] 로컬 계정 생성 창구 일원화
     */
    public static LocalAuth createLocalAuth(User user, String loginId, String encodedPassword) {
        LocalAuth localAuth = new LocalAuth();
        localAuth.user = user;
        localAuth.loginId = loginId;
        localAuth.password = encodedPassword; // 🌟 외부에서 반드시 암호화(encode) 후 넘기도록 가이드
        return localAuth;
    }

    /**
     * 🛡️ [도메인 비즈니스 메서드] 평문 오염을 방지하는 명확한 암호 변경 창구
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}