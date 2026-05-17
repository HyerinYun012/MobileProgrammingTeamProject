package com.petplace.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "마이페이지 최근 본 장소 응답 정보")
public record RecentViewResponse(
        @Schema(description = "장소 고유 ID", example = "5")
        Long restaurantId,

        @Schema(description = "장소 이름", example = "멍멍이 브런치 카페")
        String restaurantName,

        @Schema(description = "장소 카테고리", example = "카페")
        String category,

        @Schema(description = "장소 대표 이미지 URL", example = "/images/restaurants/cafe.jpg", nullable = true)
        String imageUrl,

        // 🌟 [컨벤션 수정] 파편화된 viewedAt 대신 시스템 전역 공통 필드인 createdAt 규격으로 변경합니다.
        @Schema(description = "사용자가 해당 장소를 조회한(기록이 생성된) 일시")
        LocalDateTime createdAt
) {}