package com.petplace.controller;

import com.petplace.dto.request.ReviewRequest;
import com.petplace.dto.request.ReviewReportRequest; // 💡 신규 DTO 임포트
import com.petplace.dto.response.ApiResponse;
import com.petplace.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "리뷰(Review) API", description = "장소별 리뷰 조회, 작성, 삭제 및 신고 관리 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;

    @Operation(summary = "장소별 리뷰 조회")
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<ApiResponse<List<?>>> getReviews(
            @Parameter(description = "장소(식당) ID") @PathVariable Long restaurantId
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.getReviews(restaurantId)));
    }

    /**
     * 리뷰 작성
     */
    @Operation(summary = "리뷰 작성")
    @PostMapping(value = "/restaurant/{restaurantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> write(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "장소(식당) ID") @PathVariable Long restaurantId,
            @Valid @RequestPart("request") ReviewRequest req,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        return ResponseEntity.ok(ApiResponse.success("리뷰가 등록되었습니다.", service.write(restaurantId, userId, req, image)));
    }

    @Operation(summary = "리뷰 삭제")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId
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
            @AuthenticationPrincipal Long ownerId,
            @Parameter(description = "리뷰 ID", example = "12") @PathVariable Long reviewId,
            @Valid @RequestBody ReviewReportRequest req
    ) {
        service.report(reviewId, ownerId, req.getReason());
        return ResponseEntity.ok(ApiResponse.success("신고가 정상적으로 접수되었습니다.", null));
    }
}