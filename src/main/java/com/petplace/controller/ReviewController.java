package com.petplace.controller;

import com.petplace.dto.request.ReviewRequest;
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
import java.util.Map;

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

    @Operation(summary = "리뷰 작성")
    @PostMapping(value = "/restaurant/{restaurantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> write(
            @AuthenticationPrincipal Long userId, // [수정] 인증된 유저 ID 주입
            @Parameter(description = "장소(식당) ID") @PathVariable Long restaurantId,
            @Valid @RequestPart("request") ReviewRequest req,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity.ok(ApiResponse.success("리뷰가 등록되었습니다.", service.write(restaurantId, userId, req, image)));
    }

    @Operation(summary = "리뷰 삭제")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId, // [추가] 삭제 권한 확인을 위해 주입
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId
    ) {
        service.delete(reviewId, userId); // 서비스에 userId 전달
        return ResponseEntity.ok(ApiResponse.success("리뷰가 삭제되었습니다.", null));
    }

    @Operation(summary = "리뷰 신고")
    @PostMapping("/{reviewId}/report")
    public ResponseEntity<ApiResponse<Void>> report(
            @AuthenticationPrincipal Long ownerId, // [수정] 인증된 사장님 ID 주입
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
            @RequestBody Map<String, String> req
    ) {
        service.report(reviewId, ownerId, req.get("reason"));
        return ResponseEntity.ok(ApiResponse.success("신고가 정상적으로 접수되었습니다.", null));
    }
}