package com.petplace.controller;

import com.petplace.dto.response.ApiResponse;
import com.petplace.entity.Menu;
import com.petplace.service.RestaurantMenuService;
import com.petplace.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "1. Restaurant Menu API", description = "가게 메뉴 등록, 편집 및 조회 관리 API")
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantMenuController {

    private final RestaurantMenuService restaurantMenuService;
    private final FileService fileService;

    /**
     * 식당별 전체 메뉴 목록 조회
     */
    @Operation(summary = "식당별 전체 메뉴 목록 조회 💡[신규 개설]", description = "특정 식당에 등록된 모든 메뉴 목록을 조회합니다. (비로그인 방문자도 가능)")
    @GetMapping("/{restaurantId}/menus")
    public ResponseEntity<ApiResponse<List<Menu>>> getMenus(
            @Parameter(description = "조회할 식당 고유 ID", example = "1") @PathVariable Long restaurantId) {

        List<Menu> menus = restaurantMenuService.getMenusByRestaurant(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("메뉴 목록이 성공적으로 조회되었습니다.", menus));
    }

    /**
     * 식당 메뉴 등록
     */
    @Operation(summary = "식당 메뉴 등록", description = "사장님 권한으로 식당에 제공할 음식 메뉴를 사진 파일과 함께 신규 등록합니다.")
    @PostMapping(value = "/{restaurantId}/menus", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> addMenu(
            @Parameter(description = "식당 고유 ID", example = "1") @PathVariable Long restaurantId,
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "메뉴 이름", example = "수제 견과류 쿠키") @RequestParam String name,
            @Parameter(description = "메뉴 가격 (원 단위)", example = "4500") @RequestParam int price,
            @Parameter(description = "메뉴에 대한 상세 설명", example = "댕댕이 전용 유기농 수제 쿠키입니다.") @RequestParam String description,
            @Parameter(description = "메뉴 사진 파일 (선택)")
            @RequestPart(value = "image", required = false) MultipartFile image) { // 💡 throws IOException 추가

        String imageUrl = uploadIfPresent(image);

        restaurantMenuService.registerMenu(restaurantId, currentUserId, name, price, description, imageUrl);
        return ResponseEntity.ok(ApiResponse.success("메뉴가 성공적으로 등록되었습니다.", null));
    }

    /**
     * 식당 메뉴 수정
     */
    @Operation(summary = "식당 메뉴 수정", description = "메뉴의 정보 및 사진을 수정하며, 사진 변경 시 기존 S3 파일은 삭제 연동됩니다.")
    @PutMapping(value = "/menus/{menuId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateMenu(
            @Parameter(description = "수정할 메뉴 ID", example = "10") @PathVariable Long menuId,
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "변경할 메뉴 이름") @RequestParam String name,
            @Parameter(description = "변경할 메뉴 가격") @RequestParam int price,
            @Parameter(description = "변경할 메뉴 설명") @RequestParam String description,
            @Parameter(description = "새로운 메뉴 사진 파일 (선택)")
            @RequestPart(value = "image", required = false) MultipartFile image) { // 💡 throws IOException 추가

        String imageUrl = uploadIfPresent(image);

        restaurantMenuService.updateMenu(menuId, currentUserId, name, price, description, imageUrl);
        return ResponseEntity.ok(ApiResponse.success("메뉴 정보가 성공적으로 수정되었습니다.", null));
    }

    /**
     * 식당 메뉴 삭제
     */
    @Operation(summary = "식당 메뉴 삭제", description = "식당 메뉴를 제거하며, 메뉴에 등록되어 있던 S3 파일도 깨끗하게 동시 파괴됩니다.")
    @DeleteMapping("/menus/{menuId}")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(
            @Parameter(description = "삭제할 메뉴 ID", example = "10") @PathVariable Long menuId,
            @AuthenticationPrincipal Long currentUserId) {

        restaurantMenuService.deleteMenu(menuId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("메뉴가 성공적으로 삭제되었습니다.", null));
    }

    /**
     * 파일이 존재할 경우에만 S3에 업로드하고 주소를 반환합니다.
     */
    private String uploadIfPresent(MultipartFile file) { // 💡 throws IOException 추가
        if (file != null && !file.isEmpty()) {
            return fileService.uploadFile(file);
        }
        return null;
    }
}