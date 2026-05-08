package com.petplace.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name = "recent_searches",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","keyword"}))
@Getter @Setter @NoArgsConstructor
public class RecentSearch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 100) private String keyword;
    private LocalDateTime searchedAt = LocalDateTime.now();
}
