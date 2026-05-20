package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "search_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    public SearchLog(String keyword) {
        this.keyword = keyword;
    }
}