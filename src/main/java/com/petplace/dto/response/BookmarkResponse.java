package com.petplace.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 북마크 장소(식당/카페) 응답 정보")
public record BookmarkResponse(
        @Schema(description = "장소 고유 ID", example = "5")
        Long restaurantId,

        @Schema(description = "장소 이름", example = "멍멍이 브런치 카페")
        String restaurantName,

        @Schema(description = "장소 카테고리", example = "카페")
        String category,

        @Schema(description = "장소 지번/도로명 주소", example = "서울시 강남구 역삼동 123-4")
        String address,

        @Schema(description = "장소 대표 이미지 URL", example = "/images/restaurants/cafe.jpg", nullable = true)
        String imageUrl
) {}