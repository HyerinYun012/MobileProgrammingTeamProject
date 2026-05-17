package com.petplace.dto.response;

import com.petplace.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public class CommentResponse {

    @Schema(description = "댓글 ID", example = "12")
    private final Long id;

    @Schema(description = "댓글 작성자 ID", example = "1")
    private final Long userId;

    @Schema(description = "댓글 작성자 닉네임", example = "해피맘")
    private final String nickname;

    @Schema(description = "댓글 내용", example = "강아지가 정말 귀엽네요!")
    private final String content;

    @Schema(description = "댓글 작성 시간", example = "2026-05-17T15:45:00")
    private final LocalDateTime createdAt;

    @Schema(description = "본인이 작성한 댓글 여부", example = "true")
    private final boolean isMine;

    @Schema(description = "대댓글(자식 댓글) 리스트")
    // 🌟 [수정] final을 유지하되, 서비스 레이어에서 트리 구조를 안전하게 주입할 수 있도록 빈 컬렉션으로 초기화합니다.
    private final List<CommentResponse> children = new ArrayList<>();

    /**
     * 💡 [고도화] 엔티티를 DTO로 변환하는 순수 역할만 수행 (N+1 재귀 유발 코드 전면 제거)
     */
    public CommentResponse(Comment comment, Long loginUserId) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.createdAt = comment.getCreatedAt();

        if (comment.getUser() != null) {
            this.userId = comment.getUser().getId();
            this.nickname = comment.getUser().getNickname();
            this.isMine = Objects.equals(this.userId, loginUserId);
        } else {
            this.userId = null;
            this.nickname = "알 수 없는 사용자";
            this.isMine = false;
        }
    }
}