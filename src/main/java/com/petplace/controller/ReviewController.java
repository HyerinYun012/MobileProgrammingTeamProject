package com.petplace.controller;

import com.petplace.dto.request.ReviewRequest;
import com.petplace.dto.request.ReviewReportRequest; // 💡 신규 DTO 임포트 유지
import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.ReviewResponse;
import com.petplace.service.ReviewService;
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

@Tag(name = "리뷰(Review) API", description = "장소별 리뷰 조회, 작성, 삭제 및 신고 관리 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService service;

    @Operation(summary = "식당 리뷰 목록 조회", description = "특정 식당의 리뷰를 페이징하여 조회합니다.")
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviews(
            @PathVariable Long restaurantId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReviewResponse> response = service.getReviews(restaurantId, pageable);
        return ResponseEntity.ok(ApiResponse.success("리뷰 목록 조회가 완료되었습니다.", response));
    }

    /**
     * 리뷰 작성
     */
    @Operation(summary = "리뷰 작성", description = "하나의 Form-Data 폼 안에 리뷰 평점, 텍스트 내용, 이미지 파일(imageFile)을 모아 전송합니다.")
    @PostMapping(value = "/restaurant/{restaurantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> write(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "장소(식당) ID") @PathVariable Long restaurantId,
            @Valid @ModelAttribute ReviewRequest req // 💡 MultipartFile이 내포된 DTO로 일괄 매핑 바인딩
    ) {
        // 서비스 레이어 호출 시, 개편된 DTO 사양에 맞춰 내부에 안착한 req.getImageFile()을 꺼내 넘겨줍니다.
        Object result = service.write(restaurantId, userId, req, req.getImageFile());
        return ResponseEntity.ok(ApiResponse.success("리뷰가 등록되었습니다.", result));
    }

    @Operation(summary = "리뷰 삭제", description = "본인이 작성한 리뷰를 영구 삭제합니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId
    ) {
        service.delete(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success("리뷰가 삭제되었습니다.", null));
    }

    /**
     * 리뷰 수정
     */
    @Operation(summary = "리뷰 수정", description = "본인이 작성한 리뷰를 수정합니다. 평점, 내용, 이미지 파일(imageFile)을 한데 모아 Form-Data로 전송합니다.")
    @PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> update(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
            @Valid @ModelAttribute ReviewRequest req
    ) {
        service.update(reviewId, userId, req, req.getImageFile());
        return ResponseEntity.ok(ApiResponse.success("리뷰가 수정되었습니다.", null));
    }

    /**
     * 리뷰 신고
     * 💡 파일 처리가 없는 순수 데이터 트래픽이므로 Restful 규칙에 맞게 @RequestBody 표준 형식을 유지합니다.
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