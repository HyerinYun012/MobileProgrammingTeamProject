package com.petplace.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "local_auth") @Getter @Setter @NoArgsConstructor
public class LocalAuth {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
    @Column(nullable = false, unique = true, length = 50) private String loginId;
    @Column(nullable = false, length = 255) private String password;
    @CreationTimestamp private LocalDateTime createdAt;
}
