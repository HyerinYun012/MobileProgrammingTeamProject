package com.petplace.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "마이페이지 최근 본 장소 응답 정보")
public class RecentViewResponse {

        @Schema(description = "장소 고유 ID", example = "5")
        private Long restaurantId;

        @Schema(description = "장소 이름", example = "멍멍이 브런치 카페")
        private String restaurantName;

        @Schema(description = "장소 카테고리", example = "카페")
        private String category;

        @Schema(description = "장소 대표 이미지 URL", example = "/images/restaurants/cafe.jpg", nullable = true)
        private String imageUrl;

        @Schema(description = "사용자가 해당 장소를 조회한(기록이 생성된) 일시")
        private LocalDateTime createdAt;

        // 💡 [추가] 북마크 여부 반환 필드
        @Schema(description = "해당 장소 북마크 여부", example = "true")
        private boolean isBookmarked;

        /**
         * 💡 기존 record의 생성자 호출 방식과 호환되도록 제공하는 커스텀 생성자
         * (RecentViewService 등에서 객체 생성 시 오류 발생 방지용)
         */
        public RecentViewResponse(Long restaurantId, String restaurantName, String category, String imageUrl, LocalDateTime createdAt) {
                this.restaurantId = restaurantId;
                this.restaurantName = restaurantName;
                this.category = category;
                this.imageUrl = imageUrl;
                this.createdAt = createdAt;
        }
}