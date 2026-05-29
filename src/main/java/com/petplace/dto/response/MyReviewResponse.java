package com.petplace.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "마이페이지 내가 작성한 리뷰 응답 정보")
public record MyReviewResponse(
        @Schema(description = "리뷰 고유 ID", example = "101")
        Long reviewId,

        @Schema(description = "장소 고유 ID", example = "5")
        Long restaurantId,

        @Schema(description = "내가 리뷰를 남긴 장소 이름", example = "멍멍이 브런치 카페")
        String restaurantName,

        @Schema(description = "리뷰 본문 내용", example = "인테리어도 예쁘고 반려동물 식기가 있어서 편해요!")
        String content,

        @Schema(description = "부여한 평점 (별점)", example = "5")
        int rating,

        @Schema(description = "리뷰 등록 일시", example = "2026-05-29T14:00:00") // 🌟 프론트를 위한 날짜 예시 추가
        LocalDateTime createdAt,

        @Schema(description = "리뷰 이미지 URL (등록 안 된 경우 null)", example = "https://petplace-bucket.s3.amazonaws.com/reviews/uuid_image.jpg") // 🌟 설명 및 예시 경로를 '메뉴'에서 '리뷰'로 수정
        String imageUrl
) {}