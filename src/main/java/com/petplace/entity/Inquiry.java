package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inquiries")
@Getter
@Setter
@NoArgsConstructor
// 🌟 생성 시간 및 전역 Auditing 시스템 연동을 위해 BaseTimeEntity를 상속받습니다.
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
     * 💡 [유지 및 고도화] 정적 팩토리 메서드 규칙
     * 객체 생성 시점의 도메인 규칙(초기 상태 PENDING 강제 등)을 캡슐화합니다.
     */
    public static Inquiry createInquiry(User user, Category category, String content, String email, String imageUrl) {
        Inquiry inquiry = new Inquiry();
        inquiry.setUser(user);
        inquiry.setCategory(category);
        inquiry.setContent(content);
        inquiry.setEmail(email);
        inquiry.setImageUrl(imageUrl);
        inquiry.setStatus(Status.PENDING); // 생성 시 초기 상태 지정
        return inquiry;
    }

    public void completeInquiry() {
        this.status = Status.COMPLETED;
    }

    public enum Category { GENERAL, BUSINESS, REVIEW } // 일반문의, 업장문의, 리뷰문의
    public enum Status { PENDING, COMPLETED } // 대기, 처리완료
}