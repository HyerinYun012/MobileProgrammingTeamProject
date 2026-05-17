package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
// 🌟 중복 필드 제거 및 Auditing 적용을 위해 BaseTimeEntity를 상속받습니다.
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post; // 댓글이 속한 게시글

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 댓글 글쓴이

    @Column(nullable = false, length = 1000)
    private String content; // 댓글 내용

    /**
     * 대댓글을 위한 셀프 참조 설정
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent; // 부모 댓글 (일반 댓글인 경우 null)

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children = new ArrayList<>(); // 자식 대댓글 목록

    /**
     * 💡 대댓글 양방향 연관관계 편의 메서드
     * 외부에서 세터를 조작하다가 부모-자식 관계가 깨지는 현상을 미연에 방지합니다.
     */
    public void addChildComment(Comment child) {
        this.children.add(child);
        child.setParent(this);
    }
}