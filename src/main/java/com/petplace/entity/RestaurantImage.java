package com.petplace.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "restaurant_images") @Getter @Setter @NoArgsConstructor
public class RestaurantImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "restaurant_id", nullable = false) private Restaurant restaurant;
    @Column(nullable = false, length = 500) private String imageUrl;
    private int sortOrder = 0;
    @CreationTimestamp private LocalDateTime createdAt;
}
