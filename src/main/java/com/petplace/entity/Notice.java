package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notices")
@Getter
// ✂️ 클래스 레벨의 @Setter를 제거하여 외부에서 공지사항 데이터가 무분별하게 훼손되는 것을 방방합니다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 🛡️ 의도치 않은 빈 객체 생성을 막고 JPA 스펙을 충족합니다.
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false, length = 200)
    private String title; // 공지 제목

    @Lob
    @Column(nullable = false)
    private String content; // 설명 (본문 내용)

    @Column(length = 500)
    private String thumbnailUrl; // 공지 대표 사진

    @Column(length = 500)
    private String descriptionImageUrl; // 공지 설명 사진

    /**
     * 💡 [추가] 공지사항 생성 전용 정적 팩토리 메서드
     * Setter가 닫혔으므로, 비즈니스 레이어에서 공지사항을 생성할 때 필수 제약조건을 채우도록 강제합니다.
     */
    public static Notice createNotice(Restaurant restaurant, String title, String content, String thumbnailUrl, String descriptionImageUrl) {
        Notice notice = new Notice();
        notice.restaurant = restaurant;
        notice.title = title;
        notice.content = content;
        notice.thumbnailUrl = thumbnailUrl;
        notice.descriptionImageUrl = descriptionImageUrl;
        return notice;
    }

    /**
     * 🛡️ 공지사항 정보 수정 로직 (의도가 명확한 유일한 변경 창구)
     * 무분별한 Setter 대신, 이 메서드를 통해서만 수정 기능을 수행하여 도메인의 응집도를 높입니다.
     */
    public void updateNotice(String title, String content, String thumbnailUrl, String descriptionImageUrl) {
        this.title = title;
        this.content = content;
        this.thumbnailUrl = thumbnailUrl;
        this.descriptionImageUrl = descriptionImageUrl;
        // 정보가 수정되어 영속성 컨텍스트가 Flush될 때 부모(BaseTimeEntity)의 updatedAt이 자동 갱신됩니다.
    }
}