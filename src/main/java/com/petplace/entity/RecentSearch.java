package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "recent_searches",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "keyword"})
)
@Getter
@Setter
@NoArgsConstructor
// 🌟 중복 필드 제거와 공통 전역 Auditing 연동을 위해 BaseTimeEntity를 상속받습니다.
public class RecentSearch extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String keyword;
}