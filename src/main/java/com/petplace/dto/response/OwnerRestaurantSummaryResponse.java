package com.petplace.dto.response;

import com.petplace.entity.Restaurant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "사장님 본인 업장 요약 정보")
public class OwnerRestaurantSummaryResponse {

    @Schema(description = "업장 ID", example = "1")
    private Long id;

    @Schema(description = "업장명", example = "멍멍 플레이그라운드")
    private String name;

    @Schema(description = "카테고리", example = "카페")
    private String category;

    @Schema(description = "주소", example = "경기도 시흥시 정왕동 123")
    private String address;

    public static OwnerRestaurantSummaryResponse from(Restaurant restaurant) {
        return OwnerRestaurantSummaryResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .category(restaurant.getCategory().getDescription())
                .address(restaurant.getAddress())
                .build();
    }
}
