package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "커뮤니티 게시글 및 댓글 신고 요청 객체")
public record CommunityReportRequest(

        @NotBlank(message = "신고 사유를 입력해주세요.")
        @Schema(description = "신고 사유", example = "광고성 도배 글이며, 타인을 비방하는 무차별적인 욕설이 포함되어 있습니다.")
        String reason
) {}