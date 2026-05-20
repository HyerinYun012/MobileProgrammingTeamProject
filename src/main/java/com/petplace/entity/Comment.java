package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 🛡️ 외부 무분별 빈 생성 차단
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // 앞서 Post.java에서 다룬 지침에 따라 컬렉션의 final은 걷어내고 안전하게 관리합니다.
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children = new ArrayList<>();

    /**
     * 💡 [정적 팩토리 메서드] 댓글 생성 창구
     */
    public static Comment createComment(Post post, User user, String content) {
        Comment comment = new Comment();
        comment.post = post;
        comment.user = user;
        comment.content = content;
        return comment;
    }

    /**
     * 💡 대댓글 양방향 연관관계 편의 메서드
     * 클래스 레벨 @Setter가 없어도, 동일 클래스 스펙 내부이므로 child.parent 필드 직접 제어가 가능합니다.
     */
    public void addChildComment(Comment child) {
        this.children.add(child);
        child.parent = this; // 🌟 안전하게 연관관계 맵핑 완료
    }

    /**
     * 🛡️ [도메인 비즈니스 메서드] 오직 댓글 내용만 수정 가능하도록 제한
     */
    public void updateContent(String content) {
        if (content != null && !content.isBlank()) {
            this.content = content;
        }
    }
}