package com.petplace.dto.request;

import com.petplace.entity.OperatingHour;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Schema(description = "장소 수정 요청 정보 (Multipart 폼 데이터 내 JSON 파트)")
public class RestaurantUpdateRequest {

    @Schema(description = "장소(업체)명", example = "멍멍카페 홍대점")
    @NotBlank(message = "장소 이름은 필수입니다.")
    private String name;

    @Schema(description = "도로명 주소 전체", example = "서울시 마포구 어울마당로 123")
    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    @Schema(description = "대표 전화번호", example = "02-1234-5678")
    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;

    @Schema(description = "요일별 영업시간 설정 리스트")
    private List<OperatingHourRequest> operatingHours;

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

    /* 🌟 [추가된 필드] 프론트엔드에서 삭제하지 않고 남겨둔 기존 S3 이미지 URL 주소 리스트 */
    @Schema(description = "유지할 기존 S3 이미지 URL 목록", example = "[\"https://s3.../img1.jpg\", \"https://s3.../img2.jpg\"]")
    private List<String> existingImageUrls = null; // null=클라이언트 미전송→기존유지, []=전체삭제

    public List<OperatingHour> toOperatingHourEntities() {
        if (this.operatingHours == null) return new ArrayList<>();
        return this.operatingHours.stream()
                .map(OperatingHourRequest::toEntity)
                .collect(Collectors.toList());
    }
}