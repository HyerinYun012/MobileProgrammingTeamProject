package com.petplace.controller;

import com.petplace.dto.request.*;
import com.petplace.dto.response.*;
import com.petplace.service.BookmarkService;
import com.petplace.service.MyPageService;
import com.petplace.service.RecentViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; // 💡 Page 타입 추가
import org.springframework.data.domain.Pageable; // 💡 Pageable 타입 추가
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault; // 💡 기본 페이징 설정용
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

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

    @Operation(summary = "프로필 수정", description = "하나의 Form-Data 양식 안에 변경할 닉네임, 연락처와 실제 물리 프로필 이미지 파일(profileImage)을 실어 전송합니다.")
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @ModelAttribute UpdateProfileRequest req
    ) {
        service.updateProfile(userId, req, req.getProfileImage());
        return ResponseEntity.ok(ApiResponse.success("프로필 정보가 수정되었습니다.", null));
    }

    @Operation(summary = "북마크 목록 조회", description = "로그인한 사용자가 북마크한 장소 목록을 페이징하여 조회합니다.")
    @GetMapping("/bookmarks")
    public ResponseEntity<ApiResponse<Page<BookmarkResponse>>> bookmarks(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 수정된 BookmarkService 반영
        Page<BookmarkResponse> response = bookmarkService.getBookmarks(userId, pageable);
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

    /**
     * 최근 본 장소 목록 조회 (북마크 여부 포함 결합형 구조)
     */
    @Operation(summary = "최근 본 장소 목록 조회", description = "로그인한 사용자가 최근 본 장소 목록을 페이징하여 조회하며 각 장소의 북마크 여부가 포함됩니다.")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<Page<RecentViewResponse>>> recent(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<RecentViewResponse> response = recentViewService.getRecentViews(userId, pageable);

        Set<Long> bookmarkedRestaurantIds = bookmarkService.getBookmarkedRestaurantIds(userId);

        response.forEach(recentView -> {
            boolean isBookmarked = bookmarkedRestaurantIds.contains(recentView.getRestaurantId());
            recentView.setBookmarked(isBookmarked);
        });

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

    @Operation(summary = "내 리뷰 목록 조회", description = "로그인한 사용자가 작성한 모든 리뷰 목록을 페이징하여 조회합니다.")
    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<Page<MyReviewResponse>>> myReviews(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 💡 주의: MyPageService의 getMyReviews 메서드도 Page<MyReviewResponse>를 반환하도록 변경해야 합니다.
        Page<MyReviewResponse> response = service.getMyReviews(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("내가 작성한 리뷰 목록이 성공적으로 조회되었습니다.", response));
    }
}