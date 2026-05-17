package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 글쓴이

    @Column(nullable = false, length = 200)
    private String title; // 글 제목

    @Lob
    @Column(nullable = false)
    private String content; // 글 내용

    @Column(length = 500)
    private String imageUrl; // 글 사진 URL

    @CreationTimestamp
    private LocalDateTime createdAt; // 글쓴시간

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 해당 게시글에 달린 댓글 목록 (필요 시 양방향 매핑 조회용)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();
}