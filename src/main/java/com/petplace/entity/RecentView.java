package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recent_views", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "restaurant_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PRIVATE) // 💡 외부에서 무분별한 빌더 호출 방지
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RecentView extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    public static RecentView createRecentView(User user, Restaurant restaurant) {
        return RecentView.builder()
                .user(user)
                .restaurant(restaurant)
                .build();
    }
}