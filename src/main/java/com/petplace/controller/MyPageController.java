package com.petplace.controller;

import com.petplace.dto.request.*;
import com.petplace.service.MyPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "마이페이지(MyPage) API", description = "사용자 프로필, 북마크, 최근 본 장소, 반려동물 관리 API")
@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyPageController {
    private final MyPageService service;

    @Operation(summary = "프로필 조회", description = "사용자의 기본 정보 및 프로필 데이터를 조회합니다.")
    @GetMapping("/profile")
    public ResponseEntity<?> profile(@Parameter(description = "사용자 ID") @RequestParam Long userId) {
        return ResponseEntity.ok(service.getProfile(userId));
    }

    @Operation(summary = "프로필 수정", description = "닉네임, 사진 등 사용자 프로필 정보를 업데이트합니다.")
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @Parameter(description = "사용자 ID") @RequestParam Long userId,
            @RequestBody UpdateProfileRequest req
    ) {
        service.updateProfile(userId, req);
        return ResponseEntity.ok(Map.of("message","저장 완료"));
    }

    @Operation(summary = "북마크 목록 조회", description = "사용자가 즐겨찾기한 장소(식당/카페 등) 목록을 가져옵니다.")
    @GetMapping("/bookmarks")
    public ResponseEntity<?> bookmarks(@Parameter(description = "사용자 ID") @RequestParam Long userId) {
        return ResponseEntity.ok(service.getBookmarks(userId));
    }

    @Operation(summary = "북마크 토글", description = "특정 장소를 북마크에 추가하거나 해제합니다.")
    @PostMapping("/bookmarks/{restaurantId}")
    public ResponseEntity<?> toggleBookmark(
            @Parameter(description = "장소(식당) ID") @PathVariable Long restaurantId,
            @Parameter(description = "사용자 ID") @RequestParam Long userId
    ) {
        return ResponseEntity.ok(service.toggleBookmark(userId, restaurantId));
    }

    @Operation(summary = "최근 본 장소 조회", description = "사용자가 최근에 상세 정보를 확인한 장소 목록을 가져옵니다.")
    @GetMapping("/recent")
    public ResponseEntity<?> recent(@Parameter(description = "사용자 ID") @RequestParam Long userId) {
        return ResponseEntity.ok(service.getRecentViews(userId));
    }

    @Operation(summary = "최근 본 장소 추가", description = "특정 장소를 최근 본 장소 목록에 기록합니다.")
    @PostMapping("/recent/{restaurantId}")
    public ResponseEntity<?> addRecent(
            @Parameter(description = "장소(식당) ID") @PathVariable Long restaurantId,
            @Parameter(description = "사용자 ID") @RequestParam Long userId
    ) {
        service.addRecentView(userId, restaurantId);
        return ResponseEntity.ok(Map.of("message","기록됨"));
    }

    @Operation(summary = "내 리뷰 목록 조회", description = "사용자가 작성한 모든 리뷰를 조회합니다.")
    @GetMapping("/reviews")
    public ResponseEntity<?> myReviews(@Parameter(description = "사용자 ID") @RequestParam Long userId) {
        return ResponseEntity.ok(service.getMyReviews(userId));
    }

    @Operation(summary = "반려동물 목록 조회", description = "사용자가 등록한 반려동물 리스트를 가져옵니다.")
    @GetMapping("/pets")
    public ResponseEntity<?> pets(@Parameter(description = "사용자 ID") @RequestParam Long userId) {
        return ResponseEntity.ok(service.getPets(userId));
    }

    @Operation(summary = "반려동물 추가", description = "새로운 반려동물 정보를 등록합니다.")
    @PostMapping("/pets")
    public ResponseEntity<?> addPet(
            @Parameter(description = "사용자 ID") @RequestParam Long userId,
            @RequestBody PetRequest req
    ) {
        return ResponseEntity.ok(service.addPet(userId, req));
    }

    @Operation(summary = "반려동물 정보 수정", description = "기존에 등록된 반려동물의 정보를 수정합니다.")
    @PutMapping("/pets/{petId}")
    public ResponseEntity<?> updatePet(
            @Parameter(description = "반려동물 ID") @PathVariable Long petId,
            @RequestBody PetRequest req
    ) {
        return ResponseEntity.ok(service.updatePet(petId, req));
    }
}