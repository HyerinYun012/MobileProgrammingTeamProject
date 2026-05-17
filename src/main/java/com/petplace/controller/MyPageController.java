package com.petplace.controller;

import com.petplace.dto.request.*;
import com.petplace.dto.response.*; // 💡 명확한 응답 DTO 임포트
import com.petplace.service.BookmarkService;
import com.petplace.service.MyPageService;
import com.petplace.service.RecentViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "마이페이지(MyPage) API", description = "사용자 프로필, 북마크, 최근 본 장소 관리 API")
@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService service;
    private final BookmarkService bookmarkService;
    private final RecentViewService recentViewService;

    @Operation(summary = "프로필 조회", description = "로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> profile(
            @AuthenticationPrincipal Long userId
    ) {
        UserProfileResponse response = service.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("프로필 정보가 성공적으로 조회되었습니다.", response));
    }

    @Operation(summary = "프로필 수정")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest req
    ) {
        service.updateProfile(userId, req);
        return ResponseEntity.ok(ApiResponse.success("프로필 정보가 수정되었습니다.", null));
    }

    @Operation(summary = "북마크 목록 조회", description = "로그인한 사용자가 북마크한 장소(식당) 목록을 조회합니다.")
    @GetMapping("/bookmarks")
    public ResponseEntity<ApiResponse<List<BookmarkResponse>>> bookmarks(
            @AuthenticationPrincipal Long userId
    ) {
        List<BookmarkResponse> response = bookmarkService.getBookmarks(userId);
        return ResponseEntity.ok(ApiResponse.success("북마크 목록이 성공적으로 조회되었습니다.", response));
    }

    @Operation(summary = "북마크 토글")
    @PostMapping("/bookmarks/{restaurantId}")
    public ResponseEntity<ApiResponse<Boolean>> toggleBookmark(
            @Parameter(description = "장소(식당) ID") @PathVariable Long restaurantId,
            @AuthenticationPrincipal Long userId
    ) {
        boolean isBookmarked = bookmarkService.toggleBookmark(userId, restaurantId);
        return ResponseEntity.ok(ApiResponse.success(isBookmarked));
    }

    @Operation(summary = "최근 본 장소 조회", description = "로그인한 사용자가 최근 방문/조회한 장소 목록을 조회합니다.")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<RecentViewResponse>>> recent(
            @AuthenticationPrincipal Long userId
    ) {
        List<RecentViewResponse> response = recentViewService.getRecentViews(userId);
        return ResponseEntity.ok(ApiResponse.success("최근 본 장소 목록이 성공적으로 조회되었습니다.", response));
    }

    @Operation(summary = "최근 본 장소 추가")
    @PostMapping("/recent/{restaurantId}")
    public ResponseEntity<ApiResponse<Void>> addRecent(
            @Parameter(description = "장소(식당) ID") @PathVariable Long restaurantId,
            @AuthenticationPrincipal Long userId
    ) {
        recentViewService.addRecentView(userId, restaurantId);
        return ResponseEntity.ok(ApiResponse.success("최근 본 장소에 추가되었습니다.", null));
    }

    @Operation(summary = "내 리뷰 목록 조회", description = "로그인한 사용자가 작성한 모든 리뷰 목록을 조회합니다.")
    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<List<MyReviewResponse>>> myReviews(
            @AuthenticationPrincipal Long userId
    ) {
        List<MyReviewResponse> response = service.getMyReviews(userId);
        return ResponseEntity.ok(ApiResponse.success("내가 작성한 리뷰 목록이 성공적으로 조회되었습니다.", response));
    }
}