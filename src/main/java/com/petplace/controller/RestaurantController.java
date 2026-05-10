package com.petplace.controller;

import com.petplace.dto.request.RestaurantFilterRequest;
import com.petplace.dto.request.RestaurantRequest;
import com.petplace.dto.response.ApiResponse;
import com.petplace.entity.Restaurant;
import com.petplace.service.RestaurantService;
import com.petplace.service.SearchService; // [추가] 검색 서비스 연결
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "장소(Restaurant/Cafe) API", description = "반려견 동반 가능 장소 검색, 필터링 및 업체 등록 관리 API")
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final SearchService searchService; // [추가] 통합 검색 및 로그 처리를 위해 주입

    @Operation(summary = "내 주변 장소 조회", description = "위도/경도 기준으로 반경 내 장소를 조회합니다.")
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<Restaurant>>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3.0") double radius
    ) {
        return ResponseEntity.ok(ApiResponse.success(restaurantService.findNearby(lat, lng, radius)));
    }

    @Operation(summary = "장소 키워드 검색", description = "검색 로그를 저장하며, 로그인한 경우 최근 검색어에 반영됩니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Restaurant>>> search(
            @RequestParam String keyword,
            @AuthenticationPrincipal Long userId // [수정] 인증된 유저 ID를 서비스로 전달 (최근 검색어 저장용)
    ) {
        // [연결] SearchService의 통합 검색 호출
        return ResponseEntity.ok(ApiResponse.success(searchService.search(keyword, userId)));
    }

    @Operation(summary = "조건 필터링 검색", description = "Querydsl을 사용하여 다중 조건(주차, 대형견 등)으로 장소를 검색합니다.")
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<Restaurant>>> filter(
            @Valid @ModelAttribute RestaurantFilterRequest condition
    ) {
        return ResponseEntity.ok(ApiResponse.success(restaurantService.searchRestaurants(condition)));
    }

    @Operation(summary = "장소 상세 정보 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Restaurant>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(restaurantService.getDetail(id)));
    }

    @Operation(summary = "신규 장소 등록", description = "사장님(OWNER) 권한이 있는 계정만 등록 가능합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> register(
            @AuthenticationPrincipal Long ownerId, // SecurityContext에서 안전하게 ID 추출
            @Valid @RequestBody RestaurantRequest req
    ) {
        Long registeredId = restaurantService.register(ownerId, req);
        return ResponseEntity.ok(ApiResponse.success("장소 등록이 완료되었습니다.", registeredId));
    }

    @Operation(summary = "장소 정보 수정", description = "본인이 등록한 장소만 수정할 수 있는 소유권 검증 로직이 포함됩니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Long>> update(
            @AuthenticationPrincipal Long ownerId,
            @Parameter(description = "장소 ID") @PathVariable Long id,
            @Valid @RequestBody RestaurantRequest req
    ) {
        Long updatedId = restaurantService.update(id, ownerId, req);
        return ResponseEntity.ok(ApiResponse.success("장소 정보가 수정되었습니다.", updatedId));
    }
}