package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs")
@Getter
@Setter
@NoArgsConstructor // JPA를 위한 기본 생성자
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @CreationTimestamp
    private LocalDateTime searchedAt;

    // [추가] 편리한 객체 생성을 위한 생성자
    public SearchLog(String keyword) {
        this.keyword = keyword;
    }
}