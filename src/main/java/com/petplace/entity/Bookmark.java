package com.petplace.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "bookmarks",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","restaurant_id"}))
@Getter @Setter @NoArgsConstructor
public class Bookmark {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "restaurant_id", nullable = false) private Restaurant restaurant;
    @CreationTimestamp private LocalDateTime createdAt;
}
