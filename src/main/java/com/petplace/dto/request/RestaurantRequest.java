package com.petplace.dto.request;

import com.petplace.entity.Restaurant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "장소 등록 및 수정 요청 객체 (사장님용)")
public class RestaurantRequest {

    @Schema(description = "업체명", example = "멍멍카페 강남점")
    private String name;

    @Schema(description = "도로명 주소", example = "서울특별시 강남구 테헤란로 123")
    private String address;

    @Schema(description = "업체 전화번호", example = "02-1234-5678")
    private String phone;

    @Schema(description = "사업자 등록 번호", example = "123-45-67890")
    private String businessNo;

    @Schema(description = "장소 카테고리", example = "CAFE", allowableValues = {"RESTAURANT", "CAFE", "PARK", "HOTEL"})
    private Restaurant.Category category;

    @Schema(description = "소속 지역구", example = "GANGNAM", allowableValues = {"GANGNAM", "SEOCHO", "SONGPA", "ETC"})
    private Restaurant.Region region;

    @Schema(description = "위도 (Latitude)", example = "37.5665")
    private BigDecimal latitude;

    @Schema(description = "경도 (Longitude)", example = "126.9780")
    private BigDecimal longitude;

    @Schema(description = "안전 펜스 구비 여부")
    private boolean hasFence;

    @Schema(description = "인조 잔디 여부")
    private boolean hasArtificialGrass;

    @Schema(description = "천연 잔디 여부")
    private boolean hasNaturalGrass;

    @Schema(description = "간식 판매 여부")
    private boolean hasSnack;

    @Schema(description = "주차 가능 여부")
    private boolean hasParking;

    @Schema(description = "화장실 내부 구비 여부")
    private boolean hasRestroom;

    @Schema(description = "실내 공간 여부")
    private boolean hasIndoor;

    @Schema(description = "야외 공간 여부")
    private boolean hasOutdoor;

    @Schema(description = "소형견 동반 가능 여부")
    private boolean allowSmall;

    @Schema(description = "중형견 동반 가능 여부")
    private boolean allowMedium;

    @Schema(description = "대형견 동반 가능 여부")
    private boolean allowLarge;
}