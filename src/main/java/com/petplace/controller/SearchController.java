package com.petplace.controller;

import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.RestaurantResponse;
import com.petplace.entity.Restaurant;
import com.petplace.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "검색(Search) API", description = "통합 검색, 최근 검색어 관리 및 인기 검색어 제공 API")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService service;

    @Operation(summary = "통합 검색 실행", description = "키워드로 식당을 검색합니다. 로그인 시 최근 검색어에 저장됩니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> search(
            @RequestParam String keyword,
            @AuthenticationPrincipal Long userId,
            // 💡 페이징 파라미터 추가 (기본값 설정)
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 1. 서비스 호출 시 keyword, userId와 함께 pageable을 반드시 전달합니다.
        Page<Restaurant> result = service.search(keyword, userId, pageable);

        // 2. Page<Restaurant> -> Page<RestaurantResponse> DTO 변환
        Page<RestaurantResponse> response = result.map(RestaurantResponse::from);

        return ResponseEntity.ok(ApiResponse.success("검색이 완료되었습니다.", response));
    }

    @Operation(summary = "최근 검색어 조회", description = "인증된 사용자의 최근 검색어 목록을 가져옵니다.")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<String>>> recent(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.getRecentSearches(userId)));
    }

    @Operation(summary = "최근 검색어 개별 삭제")
    @DeleteMapping("/recent/{keyword}")
    public ResponseEntity<ApiResponse<Void>> deleteRecent(
            @PathVariable String keyword,
            @AuthenticationPrincipal Long userId
    ) {
        service.deleteRecent(userId, keyword);
        return ResponseEntity.ok(ApiResponse.success("검색어가 삭제되었습니다.", null));
    }

    @Operation(summary = "인기 검색어 조회")
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<String>>> getPopularKeywords() {
        return ResponseEntity.ok(ApiResponse.success(service.getPopularKeywords()));
    }
}