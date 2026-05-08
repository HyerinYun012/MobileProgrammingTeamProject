package com.petplace.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "search_logs") @Getter @Setter @NoArgsConstructor
public class SearchLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 100) private String keyword;
    @CreationTimestamp private LocalDateTime searchedAt;
}
