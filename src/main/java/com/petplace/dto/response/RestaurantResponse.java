package com.petplace.dto.response;

import com.petplace.entity.OperatingHour;
import com.petplace.entity.Restaurant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class RestaurantResponse {
    private Long id;
    private String name;
    private String category;
    private String region;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phone;
    private List<OperatingHour> operatingHours;
    private boolean isVerified;

    // 편의시설 및 허용 조건
    private boolean allowSmall;
    private boolean allowMedium;
    private boolean allowLarge;
    private boolean hasFence;
    private boolean hasArtificialGrass;
    private boolean hasNaturalGrass;
    private boolean hasSnack;
    private boolean hasParking;
    private boolean hasRestroom;
    private boolean hasIndoor;
    private boolean hasOutdoor;

    private String imageUrl; // 대표 이미지 1장

    // 💡 북마크 여부 필드 탑재
    private boolean isBookmarked;

    /**
     * 💡 [추가] Entity와 북마크 여부를 둘 다 인자로 받아 DTO로 변환하는 오버로딩 메서드
     */
    public static RestaurantResponse from(Restaurant restaurant, boolean isBookmarked) {
        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .category(restaurant.getCategory().getDescription()) // Enum 설명값
                .region(restaurant.getRegion().getKrName())          // Enum 한글명
                .address(restaurant.getAddress())
                .latitude(restaurant.getLatitude())
                .longitude(restaurant.getLongitude())
                .phone(restaurant.getPhone())
                .operatingHours(restaurant.getOperatingHours())
                .isVerified(restaurant.isVerified())
                .allowSmall(restaurant.isAllowSmall())
                .allowMedium(restaurant.isAllowMedium())
                .allowLarge(restaurant.isAllowLarge())
                .hasFence(restaurant.isHasFence())
                .hasArtificialGrass(restaurant.isHasArtificialGrass())
                .hasNaturalGrass(restaurant.isHasNaturalGrass())
                .hasSnack(restaurant.isHasSnack())
                .hasParking(restaurant.isHasParking())
                .hasRestroom(restaurant.isHasRestroom())
                .hasIndoor(restaurant.isHasIndoor())
                .hasOutdoor(restaurant.isHasOutdoor())
                .imageUrl(restaurant.getImageUrl())
                .isBookmarked(isBookmarked)
                .build();
    }

    public static RestaurantResponse from(Restaurant restaurant) {
        return from(restaurant, false);
    }
}