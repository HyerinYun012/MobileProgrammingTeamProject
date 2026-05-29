package com.petplace.dto.response;

import com.petplace.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Builder
@AllArgsConstructor
public class CommentResponse {

    @Schema(description = "댓글 ID", example = "12")
    private final Long id;

    @Schema(description = "댓글 작성자 ID", example = "1")
    private final Long userId; // 👈 이 필드가 있어야 합니다.

    @Schema(description = "댓글 작성자 닉네임", example = "해피맘")
    private final String nickname;

    @Schema(description = "댓글 작성자 역할 (OWNER / CUSTOMER)", example = "CUSTOMER")
    private final String role;

    @Schema(description = "댓글 작성자 프로필 URL", example = "/images/profiles/user1.png")
    private final String profileUrl;

    @Schema(description = "댓글 내용", example = "강아지가 정말 귀엽네요!")
    private final String content;

    @Schema(description = "댓글 작성 시간", example = "2026-05-17T15:45:00")
    private final LocalDateTime createdAt;

    @Schema(description = "본인이 작성한 댓글 여부", example = "true")
    private final boolean isMine;

    @Builder.Default
    @Schema(description = "대댓글(자식 댓글) 리스트")
    private final List<CommentResponse> children = new ArrayList<>();

    // 🌟 정적 팩토리 메서드 추가 (이게 없어서 'from'을 해결할 수 없다는 에러가 났습니다)
    public static CommentResponse from(Comment comment, Long loginUserId) {
        return CommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUser() != null ? comment.getUser().getId() : null)
                .nickname(comment.getUser() != null ? comment.getUser().getNickname() : "알 수 없음")
                .role(comment.getUser() != null && comment.getUser().getRole() != null
                        ? comment.getUser().getRole().name() : null)
                .profileUrl(comment.getUser() != null ? comment.getUser().getProfileUrl() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .isMine(comment.getUser() != null && Objects.equals(comment.getUser().getId(), loginUserId))
                .children(new ArrayList<>())
                .build();
    }
}