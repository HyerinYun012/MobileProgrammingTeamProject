package com.petplace.controller;
import com.petplace.dto.request.ReviewRequest;
import com.petplace.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/reviews") @RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<?> getReviews(@PathVariable Long restaurantId) { return ResponseEntity.ok(service.getReviews(restaurantId)); }

    @PostMapping("/restaurant/{restaurantId}")
    public ResponseEntity<?> write(@PathVariable Long restaurantId, @RequestParam Long userId, @RequestBody ReviewRequest req) { return ResponseEntity.ok(service.write(restaurantId, userId, req)); }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> delete(@PathVariable Long reviewId) { service.delete(reviewId); return ResponseEntity.ok(Map.of("message","삭제 완료")); }

    @PostMapping("/{reviewId}/report")
    public ResponseEntity<?> report(@PathVariable Long reviewId, @RequestParam Long ownerId, @RequestBody Map<String,String> req) { service.report(reviewId, ownerId, req.get("reason")); return ResponseEntity.ok(Map.of("message","신고 접수")); }
}
