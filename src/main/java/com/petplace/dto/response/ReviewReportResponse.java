package com.petplace.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "관리자용 리뷰 신고 응답 객체")
public record ReviewReportResponse(
        @Schema(description = "신고 ID", example = "10") Long id,
        @Schema(description = "리뷰 ID", example = "505") Long reviewId,
        @Schema(description = "신고된 리뷰 내용", example = "너무 불친절해요!") String reviewContent,
        @Schema(description = "신고한 사장님 성함", example = "김사장") String ownerName,
        @Schema(description = "신고 사유", example = "허위 사실 유포") String reason,
        @Schema(description = "신고 처리 상태", example = "대기") String status,
        @Schema(description = "신고 접수일") LocalDateTime createdAt,

        // 🌟 새로 추가된 필드
        @Schema(description = "리뷰 평점(별점)", example = "5") Integer rating,
        @Schema(description = "식당(장소) 고유 ID", example = "1") Long restaurantId
) {}