package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "bookmarks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "restaurant_id"})
)
@Getter
@Setter
@NoArgsConstructor
// 🌟 중복 필드 제거를 위해 BaseTimeEntity를 상속받습니다.
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
}