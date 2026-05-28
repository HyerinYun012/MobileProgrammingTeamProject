package com.petplace.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PostDetailResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "작성자 ID", example = "1")
    private Long userId;

    @Schema(description = "게시글 제목", example = "우리집 강아지 자랑합니다")
    private String title;

    @Schema(description = "게시글 본문", example = "새로 산 장난감을 너무 좋아하네요.")
    private String content;

    @Schema(description = "첨부 이미지 URL 목록")
    private List<String> imageUrls;

    @Schema(description = "작성자 닉네임", example = "초코언니")
    private String writerNickname;

    @Schema(description = "작성자 프로필 이미지 URL", example = "/images/profiles/choco.png")
    private String writerProfileUrl;

    @Schema(description = "작성자 역할 (CUSTOMER / OWNER)", example = "CUSTOMER")
    private String writerRole;

    @Schema(description = "생성 시간", example = "2026-05-17T15:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "댓글 및 대댓글 트리 리스트")
    private List<CommentResponse> comments;
}
