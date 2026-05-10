package com.petplace.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name = "recent_views",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","restaurant_id"}))
@Getter @Setter @NoArgsConstructor
public class RecentView {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "restaurant_id", nullable = false) private Restaurant restaurant;
    private LocalDateTime viewedAt = LocalDateTime.now();
}
