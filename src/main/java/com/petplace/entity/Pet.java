package com.petplace.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity @Table(name = "pets") @Getter @Setter @NoArgsConstructor
public class Pet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 50) private String name;
    private LocalDate birth;
    @Column(length = 50) private String breed;
    @Column(length = 500) private String imageUrl;
    @CreationTimestamp private LocalDateTime createdAt;
}
