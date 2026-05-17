package com.petplace.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "관리자 대시보드용 커뮤니티(게시글/댓글) 신고 내역 응답 정보")
public record CommunityReportResponse(

        @Schema(description = "신고 내역 고유 ID", example = "1")
        Long id,

        @Schema(description = "신고 대상 게시글 ID (댓글 신고인 경우 null 가능)", example = "12", nullable = true)
        Long postId,

        @Schema(description = "신고 대상 댓글 ID (게시글 신고인 경우 null 가능)", example = "null", nullable = true)
        Long commentId,

        @Schema(description = "신고자 닉네임", example = "초코누나")
        String reporterNickname,

        @Schema(description = "신고 사유 및 내용", example = "부적절한 광고성 도배 게시글입니다.")
        String reason,

        @Schema(description = "신고 처리 상태 (PENDING: 대기중, COMPLETED: 완료/반려)", example = "PENDING")
        String status,

        @Schema(description = "신고 접수 일시", example = "2026-05-17T20:04:32")
        LocalDateTime createdAt
) {}