package com.petplace.controller;

import com.petplace.dto.request.RestaurantFilterRequest;
import com.petplace.dto.request.RestaurantRequest;
import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.RestaurantResponse;
import com.petplace.service.RestaurantService;
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

import java.util.List;

@Tag(name = "장소(Restaurant/Cafe) API", description = "반려견 동반 가능 장소 검색, 필터링 및 업체 등록 관리 API")
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    // 💡 3. 컨트롤러 레이어 변경: Swagger UI에서 실제 @RequestPart("request") 바인딩 Key와 완전히 일치하도록 선언문 수정
    private interface MultipartRequestSpec {
        @Schema(description = "가게 등록/수정 정보 (JSON)", implementation = RestaurantRequest.class)
        RestaurantRequest getRequest(); // Swagger는 Getter 규칙(getRequest -> request)을 따라 파트명을 매핑합니다.

        @Schema(description = "장소 이미지 파일 리스트", type = "array", implementation = MultipartFile.class)
        List<MultipartFile> getImages();
    }

    @Operation(summary = "내 주변 장소 조회", description = "위도/경도 기준으로 반경 내 장소를 거리순으로 페이징 조회합니다.")
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3.0") double radius,
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Pageable plainPageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return ResponseEntity.ok(ApiResponse.success(restaurantService.findNearby(lat, lng, radius, plainPageable)));
    }

    @Operation(summary = "조건 필터링 검색", description = "다중 조건으로 장소를 페이징 검색합니다. 로그인 시 북마크 여부가 포함됩니다.")
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> filter(
            @AuthenticationPrincipal Long userId,
            @Valid @ModelAttribute RestaurantFilterRequest condition,
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

        return ResponseEntity.ok(ApiResponse.success(restaurantService.searchRestaurants(userId, condition, pageable)));
    }

    /**
     * 장소 상세 정보 조회 (비로그인 사용자 연동 지원)
     */
    @Operation(summary = "장소 상세 정보 조회", description = "가게 ID를 기반으로 상세 내용을 조회하며, 로그인 시 유저의 북마크 체크 결과가 동형 반환됩니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getDetail(
            @Parameter(description = "장소 ID") @PathVariable Long id,
            @AuthenticationPrincipal Long userId
    ) {
        RestaurantResponse response = restaurantService.getRestaurantDetail(id, userId);
        return ResponseEntity.ok(ApiResponse.success("장소 상세 정보가 성공적으로 조회되었습니다.", response));
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
                            schema = @Schema(implementation = MultipartRequestSpec.class)
                    )
            )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> register(
            @AuthenticationPrincipal Long ownerId,
            @Valid @RequestPart("data") RestaurantRequest req, // 💡 "request" -> "data"로 변경
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
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
            @Valid @RequestPart("request") RestaurantRequest req, // 💡 JSON 분리 데이터 파트 ("request")
            @RequestPart(value = "images", required = false) List<MultipartFile> images // 💡 다중 파일 파트 ("images")
    ) {
        Long updatedId = restaurantService.update(id, ownerId, req, images);
        return ResponseEntity.ok(ApiResponse.success("장소 정보가 수정되었습니다.", updatedId));
    }
}