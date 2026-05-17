package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "search_logs")
@Getter
@Setter
@NoArgsConstructor // JPA를 위한 기본 생성자 유지
// 🌟 중복 필드 제거와 공통 전역 Auditing 연동을 위해 BaseTimeEntity를 상속받습니다.
public class SearchLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    // 편리한 객체 생성을 위한 생성자 유지
    public SearchLog(String keyword) {
        this.keyword = keyword;
    }
}