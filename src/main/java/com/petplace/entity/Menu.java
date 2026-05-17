package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "menus")
@Getter
@Setter
@NoArgsConstructor // JPA 리플렉션을 위한 기본 생성자
@AllArgsConstructor // Lombok @Builder 사용을 위해 필수 추가
@Builder // 빌더 패턴 사용을 위해 추가
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false, length = 100)
    private String name; // 음식 메뉴 이름

    @Column(nullable = false)
    private int price; // 음식 가격

    @Column(length = 500)
    private String description; // 음식 설명

    @Column(length = 500)
    private String imageUrl; // 메뉴 사진 URL

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}