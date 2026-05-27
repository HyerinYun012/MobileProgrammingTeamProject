package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "댓글 및 대댓글 작성/수정 요청 객체")
public record CommentRequest(

        @Schema(description = "대댓글용 부모 댓글 ID (일반 댓글 작성이거나 댓글 수정 시에는 null로 전송)", example = "1", nullable = true)
        Long parentId,

        @NotBlank(message = "댓글 내용을 입력해주세요.")
        @Schema(description = "댓글 내용", example = "정말 유익한 정보네요! 강아지랑 꼭 한 번 가봐야겠어요.")
        String content
) {}