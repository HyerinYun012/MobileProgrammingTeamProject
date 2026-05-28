package com.petplace.controller;

import com.petplace.dto.request.MenuRequest;
import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.MenuResponse;
import com.petplace.service.RestaurantMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "식당 메뉴(Menu) API", description = "가게별 메뉴 조회, 추가, 수정, 삭제 관리 API")
@RestController
@RequestMapping("/api/restaurants/{restaurantId}/menus")
@RequiredArgsConstructor
public class RestaurantMenuController {

    private final RestaurantMenuService restaurantMenuService;

    // 💡 3. 컨트롤러 레이어 변경: Swagger UI에서 파일과 JSON 구조가 묶여서 완벽히 표현되도록 하는 가상 스펙 명세 선언
    private interface MultipartRequestSpec {
        @Schema(description = "메뉴 등록/수정 정보 (JSON)", implementation = MenuRequest.class)
        MenuRequest getRequest(); // Getter 명명 규칙에 따라 Swagger 내부에서 파트명이 "request"로 자동 치환 연동됩니다.

        @Schema(description = "메뉴 단건 이미지 파일", type = "string", format = "binary")
        MultipartFile getImageFile();
    }

    /**
     * 특정 식당의 전체 메뉴 목록 조회
     */
    @Operation(summary = "식당별 메뉴 목록 조회", description = "특정 식당에 등록된 모든 메뉴 리스트를 페이징하여 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<MenuResponse>>> getMenusByRestaurant(
            @Parameter(description = "식당 고유 ID", example = "1") @PathVariable Long restaurantId,
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(page = 0, size = 1, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (pageable.getSort().stream().anyMatch(order -> "string".equals(order.getProperty()))) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("createdAt").descending()
            );
        }

        Page<MenuResponse> response = restaurantMenuService.getMenusByRestaurant(restaurantId, pageable);
        return ResponseEntity.ok(ApiResponse.success("메뉴 목록이 성공적으로 조회되었습니다.", response));
    }

    /**
     * 메뉴 등록 (사장님 전용)
     */
    @Operation(
            summary = "메뉴 등록",
            description = "텍스트 데이터(request 파트, JSON)와 메뉴 이미지 파일(imageFile 파트)을 분리하여 전송합니다. (OWNER 이상)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = MultipartRequestSpec.class)
                    )
            )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> registerMenu(
            @Parameter(hidden = true) @AuthenticationPrincipal Long ownerId,
            @Parameter(description = "식당 고유 ID", example = "1") @PathVariable Long restaurantId,
            // 💡 규칙 3 적용: 파트 이름을 프로젝트 표준 규칙인 "request"로 전면 전환
            @Valid @RequestPart("request") MenuRequest req,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        Long menuId = restaurantMenuService.registerMenu(restaurantId, ownerId, req, imageFile);
        return ResponseEntity.ok(ApiResponse.success("메뉴가 성공적으로 등록되었습니다.", menuId));
    }

    /**
     * 메뉴 수정 (사장님 전용)
     */
    @Operation(
            summary = "메뉴 정보 수정",
            description = "메뉴의 정보를 수정합니다. 텍스트는 request 파트 JSON을 사용하며, 파일 첨부 시 S3에서 자동 물리 교체됩니다. (OWNER 이상)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = MultipartRequestSpec.class)
                    )
            )
    )
    @PutMapping(value = "/{menuId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateMenu(
            @Parameter(hidden = true) @AuthenticationPrincipal Long ownerId,
            @Parameter(description = "식당 고유 ID", example = "1") @PathVariable Long restaurantId,
            @Parameter(description = "메뉴 고유 ID", example = "10") @PathVariable Long menuId,
            // 💡 규칙 3 적용: 수정 API 파트 네이밍도 동일하게 "request" 표준 구조로 매핑 전환
            @Valid @RequestPart("request") MenuRequest req,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        restaurantMenuService.updateMenu(menuId, ownerId, req, imageFile);
        return ResponseEntity.ok(ApiResponse.success("메뉴 정보가 성공적으로 수정되었습니다.", null));
    }

    /**
     * 메뉴 삭제 (사장님 전용)
     */
    @Operation(summary = "메뉴 삭제", description = "메뉴를 삭제합니다. DB 삭제 성공 시 S3 버킷 내 이미지도 영구 제거됩니다. (OWNER 이상)")
    @DeleteMapping("/{menuId}")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(
            @Parameter(hidden = true) @AuthenticationPrincipal Long ownerId,
            @Parameter(description = "식당 고유 ID", example = "1") @PathVariable Long restaurantId,
            @Parameter(description = "메뉴 고유 ID", example = "10") @PathVariable Long menuId
    ) {
        restaurantMenuService.deleteMenu(menuId, ownerId);
        return ResponseEntity.ok(ApiResponse.success("메뉴가 성공적으로 삭제되었습니다.", null));
    }
}