package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "장소 검색 필터 요청 객체")
public class RestaurantFilterRequest {

    // 💡 단일 String에서 List<String>으로 변경
    @Schema(description = "지역 목록 (다중 선택 가능, ENUM 이름 기준)", example = "[\"DAEYA\", \"SINCHEON\"]")
    private List<String> regions;

    // --- 반려견 크기별 필터 ---
    @Schema(description = "소형견(10kg 미만) 동반 가능 여부", example = "true")
    private Boolean allowSmall;

    @Schema(description = "중형견(10~25kg) 동반 가능 여부", example = "false")
    private Boolean allowMedium;

    @Schema(description = "대형견(25kg 이상) 동반 가능 여부", example = "false")
    private Boolean allowLarge;

    // --- 시설 및 서비스 필터 ---
    @Schema(description = "주차 가능 여부", example = "true")
    private Boolean hasParking;

    @Schema(description = "화장실 내부 구비 여부", example = "true")
    private Boolean hasRestroom;

    @Schema(description = "안전 펜스 설치 여부", example = "true")
    private Boolean hasFence;

    @Schema(description = "인조 잔디 여부", example = "false")
    private Boolean hasArtificialGrass;

    @Schema(description = "천연 잔디 여부", example = "true")
    private Boolean hasNaturalGrass;

    @Schema(description = "강아지 전용 간식 판매 여부", example = "true")
    private Boolean hasSnack;

    @Schema(description = "실내 공간 존재 여부", example = "true")
    private Boolean hasIndoor;

    @Schema(description = "야외 공간(테라스/운동장) 존재 여부", example = "true")
    private Boolean hasOutdoor;
}