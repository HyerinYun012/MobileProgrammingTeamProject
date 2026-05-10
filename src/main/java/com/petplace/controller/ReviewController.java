package com.petplace.controller;

import com.petplace.dto.request.ReviewRequest;
import com.petplace.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "리뷰(Review) API", description = "장소별 리뷰 조회, 작성, 삭제 및 신고 관리 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;

    @Operation(summary = "장소별 리뷰 조회", description = "특정 식당이나 카페에 등록된 모든 리뷰 목록을 가져옵니다.")
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<?> getReviews(
            @Parameter(description = "장소(식당) ID") @PathVariable Long restaurantId
    ) {
        return ResponseEntity.ok(service.getReviews(restaurantId));
    }

    @Operation(summary = "리뷰 작성", description = "장소에 대한 평점과 리뷰 내용을 등록합니다.")
    @PostMapping("/restaurant/{restaurantId}")
    public ResponseEntity<?> write(
            @Parameter(description = "장소(식당) ID") @PathVariable Long restaurantId,
            @Parameter(description = "작성자(User) ID") @RequestParam Long userId,
            @RequestBody ReviewRequest req
    ) {
        return ResponseEntity.ok(service.write(restaurantId, userId, req));
    }

    @Operation(summary = "리뷰 삭제", description = "작성한 리뷰를 삭제합니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> delete(
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId
    ) {
        service.delete(reviewId);
        return ResponseEntity.ok(Map.of("message","삭제 완료"));
    }

    @Operation(summary = "리뷰 신고", description = "부적절한 리뷰를 사장님이 신고합니다.")
    @PostMapping("/{reviewId}/report")
    public ResponseEntity<?> report(
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
            @Parameter(description = "신고하는 사장님(Owner) ID") @RequestParam Long ownerId,
            @RequestBody Map<String,String> req
    ) {
        service.report(reviewId, ownerId, req.get("reason"));
        return ResponseEntity.ok(Map.of("message","신고 접수"));
    }
}