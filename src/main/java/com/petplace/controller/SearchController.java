package com.petplace.controller;

import com.petplace.dto.response.ApiResponse;
import com.petplace.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    @Operation(summary = "통합 검색 실행", description = "로그인한 경우 사용자 최근 검색어에 저장됩니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<?>> search(
            @RequestParam String keyword,
            @AuthenticationPrincipal Long userId // SecurityContext에서 안전하게 추출
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.search(keyword, userId)));
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