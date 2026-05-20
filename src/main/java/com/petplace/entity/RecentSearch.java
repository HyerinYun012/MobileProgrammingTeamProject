package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recent_searches", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "keyword"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentSearch extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String keyword;

    public RecentSearch(User user, String keyword) {
        this.user = user;
        this.keyword = keyword;
    }
}