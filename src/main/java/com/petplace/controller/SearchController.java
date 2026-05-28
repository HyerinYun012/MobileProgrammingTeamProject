package com.petplace.controller;

import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.RestaurantResponse;
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

    /**
     * 💡 통합 검색 실행
     */
    @Operation(summary = "통합 검색 실행", description = "키워드로 식당을 검색합니다. 검색 로그가 저장되며 로그인 시 북마크 여부 포함 및 최근 검색어에 반영됩니다. 비로그인 사용자도 검색이 가능합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> search(
            @RequestParam String keyword,
            @AuthenticationPrincipal Object principal,
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(page = 0, size = 1, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = (principal instanceof Long) ? (Long) principal : null;

        if (pageable.getSort().stream().anyMatch(order -> "string".equals(order.getProperty()))) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("createdAt").descending()
            );
        }

        Page<RestaurantResponse> response = service.search(keyword, userId, pageable);
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