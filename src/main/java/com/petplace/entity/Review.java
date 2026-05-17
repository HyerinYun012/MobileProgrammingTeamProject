package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor // JPA 프록시 조회를 위한 기본 생성자 유지
@AllArgsConstructor // 💡 Lombok @SuperBuilder 사용을 위한 모든 필드 생성자 보존
// 🌟 부모 클래스(BaseTimeEntity)의 시간 필드를 빌더에서 정상 상속받기 위해 @SuperBuilder로 리팩토링합니다.
@lombok.experimental.SuperBuilder
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false)
    private int rating;

    @Lob
    private String content;

    @Column(length = 500)
    private String imageUrl;
}