package com.petplace.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 50) private String nickname;
    @Column(length = 50) private String name;
    @Column(length = 100) private String email;
    @Column(length = 20) private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role = Role.CUSTOMER;
    @Column(length = 500) private String profileUrl;
    @Column(nullable = false) private boolean marketingAgree = false;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
    public User(Long id) { this.id = id; }
    public enum Role { CUSTOMER, OWNER }
}
