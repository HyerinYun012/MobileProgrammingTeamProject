package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "bookmarks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "restaurant_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 🛡️ JPA 스펙 준수
public class Bookmark extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    /**
     * 💡 [정적 팩토리 메서드] 북마크는 생성 시점에 연관 객체 쌍이 완벽히 고정되어야 합니다.
     */
    public static Bookmark createBookmark(User user, Restaurant restaurant) {
        Bookmark bookmark = new Bookmark();
        bookmark.user = user;
        bookmark.restaurant = restaurant;
        return bookmark;
    }
}