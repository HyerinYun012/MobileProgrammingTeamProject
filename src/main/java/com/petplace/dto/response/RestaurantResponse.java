package com.petplace.dto.response;

import com.petplace.entity.OperatingHour;
import com.petplace.entity.Restaurant;
import io.swagger.v3.oas.annotations.media.Schema; // 💡 Swagger 어노테이션 임포트
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@Schema(description = "장소(식당/카페 등) 상세 정보 응답 객체")
public class RestaurantResponse {

    @Schema(description = "장소 고유 ID", example = "1")
    private Long id;

    @Schema(description = "장소명", example = "멍멍 플레이그라운드 카페")
    private String name;

    @Schema(description = "카테고리 (Enum 설명값)", example = "카페")
    private String category;

    @Schema(description = "행정 구역 (Enum 한글명)", example = "경기")
    private String region;

    @Schema(description = "전체 도로명 주소", example = "경기도 용인시 기흥구 경기도박물관로 10")
    private String address;

    @Schema(description = "위도", example = "37.2751")
    private BigDecimal latitude;

    @Schema(description = "경도", example = "127.1105")
    private BigDecimal longitude;

    @Schema(description = "전화번호", example = "031-123-4567")
    private String phone;

    @Schema(description = "영업시간 목록")
    private List<OperatingHour> operatingHours;

    @Schema(description = "인증된 제휴 업체 여부", example = "true")
    private boolean isVerified;

    // 편의시설 및 허용 조건
    @Schema(description = "소형견 허용 여부 (~7kg)", example = "true")
    private boolean allowSmall;

    @Schema(description = "중형견 허용 여부 (7~15kg)", example = "true")
    private boolean allowMedium;

    @Schema(description = "대형견 허용 여부 (15kg~)", example = "false")
    private boolean allowLarge;

    @Schema(description = "안전 펜스 설치 여부", example = "true")
    private boolean hasFence;

    @Schema(description = "인조 잔디 여부", example = "false")
    private boolean hasArtificialGrass;

    @Schema(description = "천연 잔디 여부", example = "true")
    private boolean hasNaturalGrass;

    @Schema(description = "반려견 간식 판매 여부", example = "true")
    private boolean hasSnack;

    @Schema(description = "주차장 보유 여부", example = "true")
    private boolean hasParking;

    @Schema(description = "반려견 전용 화장실/어메니티 여부", example = "false")
    private boolean hasRestroom;

    @Schema(description = "실내 공간 보유 여부", example = "true")
    private boolean hasIndoor;

    @Schema(description = "야외 운동장/테라스 보유 여부", example = "true")
    private boolean hasOutdoor;

    @Schema(description = "장소 대표 이미지 URL", example = "https://petplace-bucket.s3.amazonaws.com/restaurants/playground_main.jpg")
    private String imageUrl;

    @Schema(description = "로그인한 유저의 북마크(즐겨찾기) 등록 여부", example = "true")
    private boolean isBookmarked;

    /**
     * Entity와 북마크 여부를 둘 다 인자로 받아 DTO로 변환하는 오버로딩 메서드
     */
    public static RestaurantResponse from(Restaurant restaurant, boolean isBookmarked) {
        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .category(restaurant.getCategory().getDescription())
                .region(restaurant.getRegion().getKrName())
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