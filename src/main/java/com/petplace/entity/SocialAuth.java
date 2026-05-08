package com.petplace.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "social_auth",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider","provider_id"}))
@Getter @Setter @NoArgsConstructor
public class SocialAuth {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Provider provider;
    @Column(nullable = false, length = 100) private String providerId;
    @CreationTimestamp private LocalDateTime createdAt;
    public enum Provider { KAKAO, NAVER }
}
