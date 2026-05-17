package com.petplace.controller;

import com.petplace.dto.request.RestaurantFilterRequest;
import com.petplace.dto.request.RestaurantRequest;
import com.petplace.dto.response.ApiResponse;
import com.petplace.entity.Restaurant;
import com.petplace.service.RestaurantService;
import com.petplace.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "장소(Restaurant/Cafe) API", description = "반려견 동반 가능 장소 검색, 필터링 및 업체 등록 관리 API")
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final SearchService searchService;

    /**
     * 💡 [스웨거 파일 업로드 명세 전용 인터페이스]
     * 복잡한 properties 배열 연산 대신, 스웨거 화면에 객체와 바이너리 필드를
     * 1:1로 깔끔하게 매핑해 주는 표준 DTO 래퍼 구조입니다.
     */
    private interface MultipartRequestSpec {
        @Schema(description = "가게 등록/수정 정보 (JSON)", implementation = RestaurantRequest.class)
        RestaurantRequest getRequest();

        @Schema(description = "장소 이미지 파일 리스트", type = "array", implementation = MultipartFile.class)
        List<MultipartFile> getImages();
    }

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
            @AuthenticationPrincipal Long userId
    ) {
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

    /**
     * 신규 장소 등록 (다중 이미지 업로드 지원)
     */
    @Operation(
            summary = "신규 장소 등록",
            description = "사장님(OWNER) 권한이 있는 계정만 등록 가능하며, 여러 장의 이미지 파일을 함께 등록할 수 있습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            // 💡 [교정] 위의 명세용 인터페이스를 바인딩하여 복잡한 호환 예외를 원천 차단합니다.
                            schema = @Schema(implementation = MultipartRequestSpec.class)
                    )
            )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> register(
            @AuthenticationPrincipal Long ownerId,
            @Valid @RequestPart("request") RestaurantRequest req,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {

        Long registeredId = restaurantService.register(ownerId, req, images);
        return ResponseEntity.ok(ApiResponse.success("장소 등록이 완료되었습니다.", registeredId));
    }

    /**
     * 장소 정보 수정 (다중 이미지 전체 교체 지원)
     */
    @Operation(
            summary = "장소 정보 수정",
            description = "본인이 등록한 장소만 수정할 수 있으며, 이미지 첨부 시 기존 S3 파일들은 전체 교체 삭제됩니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = MultipartRequestSpec.class)
                    )
            )
    )
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> update(
            @AuthenticationPrincipal Long ownerId,
            @Parameter(description = "장소 ID") @PathVariable Long id,
            @Valid @RequestPart("request") RestaurantRequest req,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {

        Long updatedId = restaurantService.update(id, ownerId, req, images);
        return ResponseEntity.ok(ApiResponse.success("장소 정보가 수정되었습니다.", updatedId));
    }
}