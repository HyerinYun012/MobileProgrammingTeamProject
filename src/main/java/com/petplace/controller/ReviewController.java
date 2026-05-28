package com.petplace.controller;

import com.petplace.dto.request.ReviewRequest;
import com.petplace.dto.request.ReviewReportRequest;
import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.ReviewResponse;
import com.petplace.service.ReviewService;
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

@Tag(name = "리뷰(Review) API", description = "장소별 리뷰 조회, 작성, 삭제 및 신고 관리 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService service;

    // 💡 Swagger UI에서 파일(imageFile)과 JSON(request)이 하나의 폼 데이터로 묶이도록 명세 정의
    private interface MultipartRequestSpec {
        @Schema(description = "리뷰 작성/수정 정보 (JSON)", implementation = ReviewRequest.class)
        ReviewRequest getData(); // 🌟 getRequest() -> getData()로 변경해야 Swagger에서 'data' 키값으로 인식합니다.

        @Schema(description = "리뷰 이미지 파일", type = "string", format = "binary")
        MultipartFile getImageFile();
    }

    /**
     * 식당 리뷰 목록 조회
     */
    @Operation(summary = "식당 리뷰 목록 조회", description = "특정 식당의 리뷰를 페이징하여 조회합니다.")
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviews(
            @Parameter(description = "조회할 식당 고유 ID", example = "1") @PathVariable Long restaurantId,
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

        Page<ReviewResponse> response = service.getReviews(restaurantId, pageable);
        return ResponseEntity.ok(ApiResponse.success("리뷰 목록 조회가 완료되었습니다.", response));
    }

    /**
     * 리뷰 작성
     */
    @Operation(
            summary = "리뷰 작성",
            description = "텍스트 데이터(data 파트, JSON)와 리뷰 이미지 파일(imageFile 파트)을 분리하여 전송합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = MultipartRequestSpec.class)
                    )
            )
    )
    @PostMapping(value = "/restaurant/{restaurantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> write(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "장소(식당) ID", example = "1") @PathVariable Long restaurantId,
            // 🌟 수정: "request" -> "data"로 변경하여 클라이언트 변수명과 일치시킵니다.
            @Valid @RequestPart("data") ReviewRequest req,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        Object result = service.write(restaurantId, userId, req, imageFile);
        return ResponseEntity.ok(ApiResponse.success("리뷰가 등록되었습니다.", result));
    }

    /**
     * 리뷰 수정
     */
    @Operation(
            summary = "리뷰 수정",
            description = "본인이 작성한 리뷰를 수정합니다. 텍스트는 data 파트 JSON을 사용하며, 파일 파트는 선택적으로 첨부합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = MultipartRequestSpec.class)
                    )
            )
    )
    @PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> update(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "수정할 리뷰 ID", example = "100") @PathVariable Long reviewId,
            // 🌟 수정: "request" -> "data"로 변경하여 클라이언트 변수명과 일치시킵니다.
            @Valid @RequestPart("data") ReviewRequest req,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        service.update(reviewId, userId, req, imageFile);
        return ResponseEntity.ok(ApiResponse.success("리뷰가 수정되었습니다.", null));
    }

    /**
     * 리뷰 삭제
     */
    @Operation(summary = "리뷰 삭제", description = "본인이 작성한 리뷰를 영구 삭제합니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "삭제할 리뷰 ID", example = "100") @PathVariable Long reviewId
    ) {
        service.delete(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success("리뷰가 삭제되었습니다.", null));
    }

    /**
     * 리뷰 신고
     */
    @Operation(summary = "리뷰 신고", description = "부적절한 리뷰를 전용 DTO 사유와 함께 접수 및 처리합니다.")
    @PostMapping("/{reviewId}/report")
    public ResponseEntity<ApiResponse<Void>> report(
            @Parameter(hidden = true) @AuthenticationPrincipal Long ownerId,
            @Parameter(description = "신고 대상 리뷰 ID", example = "12") @PathVariable Long reviewId,
            @Valid @RequestBody ReviewReportRequest req
    ) {
        service.report(reviewId, ownerId, req.getReason());
        return ResponseEntity.ok(ApiResponse.success("신고가 정상적으로 접수되었습니다.", null));
    }
}