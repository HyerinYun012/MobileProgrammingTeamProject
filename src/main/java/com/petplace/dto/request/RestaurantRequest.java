package com.petplace.dto.request;

import com.petplace.entity.Restaurant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "장소 등록 및 수정 요청 정보 (Multipart 폼 데이터 내 JSON 파트)")
public class RestaurantRequest {

    @Schema(description = "장소(업체)명", example = "멍멍카페 홍대점")
    @NotBlank(message = "장소 이름은 필수입니다.")
    private String name;

    @Schema(description = "도로명 주소 전체", example = "서울시 마포구 어울마당로 123")
    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    @Schema(description = "대표 전화번호", example = "02-1234-5678")
    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;

    @Schema(description = "사업자 등록 번호 (하이픈 포함 규격)", example = "120-00-12345")
    @NotBlank(message = "사업자 등록 번호는 필수입니다.")
    private String businessNo;

    @Schema(description = "업종 카테고리 (RESTAURANT: 식당, CAFE: 카페)", example = "CAFE")
    @NotNull(message = "카테고리는 필수입니다.")
    private Restaurant.Category category;

    @Schema(description = "소재 행정 구역 대분류 (BAEGON, JEONGWANG 등)", example = "BAEGON")
    @NotNull(message = "행정 구역 분류는 필수입니다.")
    private Restaurant.Region region;

    @Schema(description = "매장 지도 위도 (Latitude)", example = "37.556789")
    @NotNull(message = "위도 좌표는 필수입니다.")
    private BigDecimal latitude;

    @Schema(description = "매장 지도 경도 (Longitude)", example = "126.923456")
    @NotNull(message = "경도 좌표는 필수입니다.")
    private BigDecimal longitude;

    @Schema(description = "안전 펜스 울타리 설치 여부", example = "true")
    private boolean hasFence;

    @Schema(description = "인조 잔디 운동장 보유 여부", example = "false")
    private boolean hasArtificialGrass;

    @Schema(description = "천연 잔디 운동장 보유 여부", example = "true")
    private boolean hasNaturalGrass;

    @Schema(description = "반려견 전용 간식 판매 여부", example = "true")
    private boolean hasSnack;

    @Schema(description = "자체 주차장 공간 보유 여부", example = "true")
    private boolean hasParking;

    @Schema(description = "매장 내부 실내 화장실 구비 여부", example = "true")
    private boolean hasRestroom;

    @Schema(description = "실내 공간 동반 동선 허용 여부", example = "true")
    private boolean hasIndoor;

    @Schema(description = "야외 테라스/루프탑 공간 보유 여부", example = "false")
    private boolean hasOutdoor;

    @Schema(description = "소형견(10kg 미만) 입장 가능 여부", example = "true")
    private boolean allowSmall;

    @Schema(description = "중형견(10~25kg) 입장 가능 여부", example = "true")
    private boolean allowMedium;

    @Schema(description = "대형견(25kg 초과) 입장 가능 여부", example = "false")
    private boolean allowLarge;

    @Schema(description = "정규 영업 시간 안내 정보 문구 (선택 사항)", example = "매일 11:00 - 21:00")
    private String operatingHours;

    @Schema(description = "매장 시그니처 메뉴 등록 정보 목록 리스트")
    private List<MenuRequest> menus;

    /**
     * 🌟 [리팩토링 완료] 임의의 Setter 호출 대신 고도화된 전용 비즈니스 생성자를 체택하여
     * 원자적이고 일관성 있는 비즈니스 엔티티 객체를 조립해 반환합니다.
     */
    public Restaurant toEntity() {
        return new Restaurant(
                this.name,
                this.address,
                this.phone,
                this.businessNo,
                this.category,
                this.region,
                this.latitude,
                this.longitude,
                this.operatingHours,
                this.hasFence,
                this.hasArtificialGrass,
                this.hasNaturalGrass,
                this.hasSnack,
                this.hasParking,
                this.hasRestroom,
                this.hasIndoor,
                this.hasOutdoor,
                this.allowSmall,
                this.allowMedium,
                this.allowLarge
        );
    }
}