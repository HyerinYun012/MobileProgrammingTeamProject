package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 🛡️ 안전장치 확보
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(length = 100)
    private String email;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    /**
     * 💡 [정적 팩토리 메서드 고도화]
     * 내부 세터 호출을 제거하고 direct 필드 대입으로 수정하여 완전성을 확보합니다.
     */
    public static Inquiry createInquiry(User user, Category category, String content, String email, String imageUrl) {
        Inquiry inquiry = new Inquiry();
        inquiry.user = user;
        inquiry.category = category;
        inquiry.content = content;
        inquiry.email = email;
        inquiry.imageUrl = imageUrl;
        inquiry.status = Status.PENDING; // 초기 비즈니스 상태 강제 보장
        return inquiry;
    }

    /**
     * 🛡️ [도메인 비즈니스 메서드] 명확한 상태 변경 메서드 유지
     */
    public void completeInquiry() {
        this.status = Status.COMPLETED;
    }

    public enum Category { GENERAL, BUSINESS, ERROR }
    public enum Status { PENDING, COMPLETED }
}