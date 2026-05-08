package com.petplace.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "review_reports",
    uniqueConstraints = @UniqueConstraint(columnNames = {"review_id","owner_id"}))
@Getter @Setter @NoArgsConstructor
public class ReviewReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "review_id", nullable = false) private Review review;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id", nullable = false) private User owner;
    @Column(length = 200) private String reason;
    @Enumerated(EnumType.STRING) private Status status = Status.대기;
    @CreationTimestamp private LocalDateTime createdAt;
    public enum Status { 대기, 처리완료 }
}
