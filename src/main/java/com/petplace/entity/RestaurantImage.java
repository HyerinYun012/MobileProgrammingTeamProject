package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "restaurant_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private int sortOrder = 0;

    public RestaurantImage(String imageUrl, Restaurant restaurant, int sortOrder) {
        this.imageUrl = imageUrl;
        this.restaurant = restaurant;
        this.sortOrder = sortOrder;
    }
}