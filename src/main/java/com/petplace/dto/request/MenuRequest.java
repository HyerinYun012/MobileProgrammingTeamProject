package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile; // 💡 추가

@Getter @Setter
@Schema(description = "메뉴 등록 및 수정 요청 객체")
public class MenuRequest {

    @NotBlank(message = "메뉴 이름은 필수 입력 항목입니다.")
    @Size(max = 100, message = "메뉴 이름은 100자 이내여야 합니다.")
    @Schema(description = "음식 메뉴 이름", example = "수제 수박 퓨레")
    private String name;

    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    @Schema(description = "음식 가격", example = "6500")
    private int price;

    @Size(max = 500, message = "메뉴 설명은 500자 이내여야 합니다.")
    @Schema(description = "음식 메뉴 세부 설명", example = "신선한 수박을 그대로 갈아 만든 반려견 전용 특식 디저트")
    private String description;

    @Schema(description = "새로 등록하거나 교체할 메뉴 물리 이미지 파일 (선택 사항)")
    private MultipartFile imageFile; // ⭕ [교정] 폼 데이터를 다이렉트로 매핑받기 위한 물리 파일 필드 추가
}