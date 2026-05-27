package com.petplace.dto.response;

import com.petplace.entity.Menu;
import io.swagger.v3.oas.annotations.media.Schema; // 💡 Swagger 어노테이션 임포트
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "장소별 메뉴 정보 응답 객체") // 💡 클래스 단위 설명
public class MenuResponse {

    @Schema(description = "메뉴 고유 ID", example = "15")
    private Long id;

    @Schema(description = "메뉴 이름", example = "멍푸치노 (락토프리)")
    private String name;

    @Schema(description = "메뉴 가격 (원 단위)", example = "4500")
    private Integer price;

    @Schema(description = "메뉴 상세 설명", example = "반려견을 위한 락토프리 우유와 캐롭 파우더로 만든 시그니처 음료")
    private String description;

    @Schema(description = "메뉴 이미지 URL (등록 안 된 경우 null)", example = "https://petplace-bucket.s3.amazonaws.com/menus/puppuccino.jpg")
    private String imageUrl;

    public static MenuResponse from(Menu menu) {
        return MenuResponse.builder()
                .id(menu.getId())
                .name(menu.getName())
                .price(menu.getPrice())
                .description(menu.getDescription())
                .imageUrl(menu.getImageUrl())
                .build();
    }
}