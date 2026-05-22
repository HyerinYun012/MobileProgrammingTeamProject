package com.petplace.controller;

import com.petplace.dto.request.MenuRequest;
import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.MenuResponse;
import com.petplace.service.RestaurantMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "식당 메뉴(Menu) API", description = "가게별 메뉴 조회, 추가, 수정, 삭제 관리 API")
@RestController
@RequestMapping("/api/restaurants/{restaurantId}/menus")
@RequiredArgsConstructor
public class RestaurantMenuController {

    private final RestaurantMenuService restaurantMenuService;

    /**
     * 💡 특정 식당의 전체 메뉴 목록 조회
     */
    @Operation(summary = "식당별 메뉴 목록 조회", description = "특정 식당에 등록된 모든 메뉴 리스트를 페이징하여 조회합니다.")
    @GetMapping // 💡 경로를 비워두면 @RequestMapping의 경로 그대로 매핑됩니다.
    public ResponseEntity<ApiResponse<Page<MenuResponse>>> getMenusByRestaurant(
            @Parameter(description = "식당 고유 ID") @PathVariable Long restaurantId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MenuResponse> response = restaurantMenuService.getMenusByRestaurant(restaurantId, pageable);
        return ResponseEntity.ok(ApiResponse.success("메뉴 목록이 성공적으로 조회되었습니다.", response));
    }

    /**
     * 메뉴 등록 (사장님 전용)
     */
    @Operation(summary = "메뉴 등록", description = "하나의 Form-Data 폼 안에 메뉴 텍스트 정보와 이미지 파일(imageFile)을 모아 전송합니다. (OWNER 이상)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> registerMenu(
            @AuthenticationPrincipal Long ownerId,
            @Parameter(description = "식당 고유 ID") @PathVariable Long restaurantId,
            @Valid @ModelAttribute MenuRequest req // 💡 MultipartFile이 내재된 DTO를 통째로 바인딩
    ) {
        // 리팩토링된 서비스 시그니처에 맞춰 DTO와 내부 파일 객체를 정교하게 분리 인계합니다.
        Long menuId = restaurantMenuService.registerMenu(restaurantId, ownerId, req, req.getImageFile());
        return ResponseEntity.ok(ApiResponse.success("메뉴가 성공적으로 등록되었습니다.", menuId));
    }

    /**
     * 메뉴 수정 (사장님 전용)
     */
    @Operation(summary = "메뉴 정보 수정", description = "메뉴의 정보를 수정합니다. 새로운 이미지 파일을 첨부하면 S3에서 자동 물리 교체됩니다. (OWNER 이상)")
    @PutMapping(value = "/{menuId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateMenu(
            @AuthenticationPrincipal Long ownerId,
            @Parameter(description = "식당 고유 ID") @PathVariable Long restaurantId, // URL 정합성 유지
            @Parameter(description = "메뉴 고유 ID") @PathVariable Long menuId,
            @Valid @ModelAttribute MenuRequest req // 💡 수정 데이터 및 신규 교체 파일 통합 바인딩
    ) {
        restaurantMenuService.updateMenu(menuId, ownerId, req, req.getImageFile());
        return ResponseEntity.ok(ApiResponse.success("메뉴 정보가 성공적으로 수정되었습니다.", null));
    }

    /**
     * 메뉴 삭제 (사장님 전용)
     */
    @Operation(summary = "메뉴 삭제", description = "메뉴를 삭제합니다. DB 삭제 성공 시 S3 버킷 내 이미지도 영구 제거됩니다. (OWNER 이상)")
    @DeleteMapping("/{menuId}")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(
            @AuthenticationPrincipal Long ownerId,
            @Parameter(description = "식당 고유 ID") @PathVariable Long restaurantId, // URL 정합성 유지
            @Parameter(description = "메뉴 고유 ID") @PathVariable Long menuId
    ) {
        restaurantMenuService.deleteMenu(menuId, ownerId);
        return ResponseEntity.ok(ApiResponse.success("메뉴가 성공적으로 삭제되었습니다.", null));
    }
}