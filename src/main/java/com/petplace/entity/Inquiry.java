package com.petplace.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "inquiries") @Getter @Setter @NoArgsConstructor
public class Inquiry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) private Category category = Category.일반문의;
    @Column(length = 100) private String email;
    @Lob @Column(nullable = false) private String content;
    @Lob
    private String answer;
    private boolean isAnswered = false;
    private LocalDateTime answeredAt;
    @Column(length = 500) private String imageUrl;
    @Enumerated(EnumType.STRING) private Status status = Status.대기;
    @CreationTimestamp private LocalDateTime createdAt;
    public enum Category { 일반문의, 업장문의, 리뷰문의 }
    public enum Status { 대기, 처리완료 }
}
