package com.petplace.controller;

import com.petplace.dto.request.*;
import com.petplace.dto.response.*;
import com.petplace.service.BookmarkService;
import com.petplace.service.MyPageService;
import com.petplace.service.RecentViewService;
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

@Tag(name = "마이페이지(MyPage) API", description = "사용자 프로필, 북마크, 최근 본 장소 관리 API")
@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService service;
    private final BookmarkService bookmarkService;
    private final RecentViewService recentViewService;

    // 💡 3. 컨트롤러 레이어 변경: Swagger UI에서 JSON 데이터와 단건 파일 파트를 매핑해주는 가상 명세 인터페이스
    private interface ProfileUpdateRequestSpec {
        @Schema(description = "프로필 수정 텍스트 정보 (JSON)", implementation = UpdateProfileRequest.class)
        UpdateProfileRequest getRequest();

        @Schema(description = "새로운 프로필 이미지 파일 (선택)", type = "string", format = "binary")
        MultipartFile getProfileImage();
    }

    @Operation(summary = "프로필 조회", description = "로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> profile(
            @AuthenticationPrincipal Long userId
    ) {
        UserProfileResponse response = service.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("프로필 정보가 성공적으로 조회되었습니다.", response));
    }

    /**
     * 프로필 수정 (Multipart/Form-Data 적용 및 표준화 명세 연동)
     */
    @Operation(
            summary = "프로필 수정",
            description = "텍스트 데이터(request 파트, JSON)와 실제 물리 프로필 이미지 파일(profileImage 파트)을 분리하여 전송합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = ProfileUpdateRequestSpec.class)
                    )
            )
    )
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestPart("request") UpdateProfileRequest req, // 💡 규칙에 따라 키값을 "data" -> "request"로 표준화 변경
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        service.updateProfile(userId, req, profileImage);
        return ResponseEntity.ok(ApiResponse.success("프로필 정보가 수정되었습니다.", null));
    }

    @Operation(summary = "북마크 목록 조회", description = "로그인한 사용자가 북마크한 장소 목록을 페이징하여 조회합니다.")
    @GetMapping("/bookmarks")
    public ResponseEntity<ApiResponse<Page<BookmarkResponse>>> bookmarks(
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

    @Operation(summary = "최근 본 장소 목록 조회", description = "로그인한 사용자가 최근 본 장소 목록을 페이징하여 조회하며 각 장소의 북마크 여부가 포함됩니다.")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<Page<RecentViewResponse>>> recent(
            @AuthenticationPrincipal Long userId,
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

        Page<RecentViewResponse> response = recentViewService.getRecentViews(userId, pageable);
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

        Page<MyReviewResponse> response = service.getMyReviews(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("내가 작성한 리뷰 목록이 성공적으로 조회되었습니다.", response));
    }
}