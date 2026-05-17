package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 🛡️ JPA 스펙 준수 및 외부에서 new Post() 전면 차단
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 💡 외부에서 모든 필드 생성자를 직접 찌르는 행위 방지
@lombok.experimental.SuperBuilder
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(length = 500)
    private String imageUrl;

    /**
     * 🌟 [경고 해결 및 안전성 확보]
     * 1. 롬복 빌더 사용 시 컬렉션 주소 자체가 갈아끼워져 하이버네이트 프록시가 깨지는 것을 막기 위해 final로 선언합니다.
     * 2. @Builder.Default와 final을 함께 사용하여 빌더 호출 시에도 내장 ArrayList 주소 컨텐트가 무결하게 유지되도록 보장합니다.
     */
    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<Comment> comments = new ArrayList<>();

    // Setter를 열어두는 대신 비즈니스 수정을 위한 전용 도메인 메서드 제공
    public void updateContent(String title, String content, String imageUrl) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
    }
}