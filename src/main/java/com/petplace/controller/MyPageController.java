package com.petplace.controller;

import com.petplace.dto.request.*;
import com.petplace.dto.response.ApiResponse;
import com.petplace.service.MyPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "마이페이지(MyPage) API", description = "사용자 프로필, 북마크, 최근 본 장소, 반려동물 관리 API")
@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyPageController {
    private final MyPageService service;

    @Operation(summary = "프로필 조회", description = "로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<?>> profile(
            @AuthenticationPrincipal Long userId // [수정] 인증된 사용자 ID 직접 사용
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.getProfile(userId)));
    }

    @Operation(summary = "프로필 수정")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal Long userId, // [수정] 외부 파라미터가 아닌 인증 정보 사용
            @Valid @RequestBody UpdateProfileRequest req
    ) {
        service.updateProfile(userId, req);
        return ResponseEntity.ok(ApiResponse.success("프로필 정보가 수정되었습니다.", null));
    }

    @Operation(summary = "북마크 목록 조회")
    @GetMapping("/bookmarks")
    public ResponseEntity<ApiResponse<?>> bookmarks(
            @AuthenticationPrincipal Long userId // [수정]
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.getBookmarks(userId)));
    }

    @Operation(summary = "북마크 토글")
    @PostMapping("/bookmarks/{restaurantId}")
    public ResponseEntity<ApiResponse<Boolean>> toggleBookmark(
            @Parameter(description = "장소(식당) ID") @PathVariable Long restaurantId,
            @AuthenticationPrincipal Long userId // [수정]
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.toggleBookmark(userId, restaurantId)));
    }

    @Operation(summary = "최근 본 장소 조회")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<?>> recent(
            @AuthenticationPrincipal Long userId // [수정]
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.getRecentViews(userId)));
    }

    @Operation(summary = "최근 본 장소 추가")
    @PostMapping("/recent/{restaurantId}")
    public ResponseEntity<ApiResponse<Void>> addRecent(
            @Parameter(description = "장소(식당) ID") @PathVariable Long restaurantId,
            @AuthenticationPrincipal Long userId // [수정]
    ) {
        service.addRecentView(userId, restaurantId);
        return ResponseEntity.ok(ApiResponse.success("최근 본 장소에 추가되었습니다.", null));
    }

    @Operation(summary = "내 리뷰 목록 조회")
    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<?>> myReviews(
            @AuthenticationPrincipal Long userId // [수정]
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.getMyReviews(userId)));
    }

    @Operation(summary = "반려동물 목록 조회")
    @GetMapping("/pets")
    public ResponseEntity<ApiResponse<?>> pets(
            @AuthenticationPrincipal Long userId // [수정]
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.getPets(userId)));
    }

    @Operation(summary = "반려동물 추가")
    @PostMapping("/pets")
    public ResponseEntity<ApiResponse<?>> addPet(
            @AuthenticationPrincipal Long userId, // [수정]
            @Valid @RequestBody PetRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.addPet(userId, req)));
    }

    @Operation(summary = "반려동물 정보 수정", description = "반려동물의 정보를 수정합니다. 본인의 반려동물인지 검증이 포함됩니다.")
    @PutMapping("/pets/{petId}")
    public ResponseEntity<ApiResponse<?>> updatePet(
            @AuthenticationPrincipal Long userId, // [유지] 이미 잘 적용되어 있음
            @Parameter(description = "반려동물 ID") @PathVariable Long petId,
            @Valid @RequestBody PetRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.updatePet(userId, petId, req)));
    }
}