package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 💡 내부 빌더용 전 필드 생성자
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_images", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "image_url", length = 1000)
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    // 도메인 비즈니스 수정을 위한 전용 메서드 (newImageUrls == null 이면 기존 사진 유지)
    public void updateContent(String title, String content, List<String> newImageUrls) {
        this.title = title;
        this.content = content;
        if (newImageUrls != null) {
            this.imageUrls.clear();
            this.imageUrls.addAll(newImageUrls);
        }
    }
}