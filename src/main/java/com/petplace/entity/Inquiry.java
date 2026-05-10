package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "inquiries")
@Getter @Setter @NoArgsConstructor
public class Inquiry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // fetch 타입을 LAZY로 두어 컨트롤러-서비스 간 지연 로딩 최적화 유지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) // 기본값이 있더라도 DB 제약조건 추가 권장
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

    @CreationTimestamp
    @Column(updatable = false) // 생성 시간은 수정 불가능하게 설정
    private LocalDateTime createdAt;

    // 서비스 레이어에서 사용하기 편하도록 생성자 혹은 정적 팩토리 메서드 권장
    public static Inquiry createInquiry(User user, Category category, String content, String email, String imageUrl) {
        Inquiry inquiry = new Inquiry();
        inquiry.user = user;
        inquiry.category = category;
        inquiry.content = content;
        inquiry.email = email;
        inquiry.imageUrl = imageUrl;
        inquiry.status = Status.PENDING; // 상태 강제 초기화
        return inquiry;
    }

    // Enum 명칭은 영어로, 의미는 주석으로 관리
    public enum Category { GENERAL, BUSINESS, REVIEW } // 일반문의, 업장문의, 리뷰문의
    public enum Status { PENDING, COMPLETED } // 대기, 처리완료
}