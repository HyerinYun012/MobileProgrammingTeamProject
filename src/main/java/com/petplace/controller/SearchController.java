package com.petplace.controller;

import com.petplace.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "검색(Search) API", description = "통합 검색, 최근 검색어 관리 및 추천 검색어 제공 API")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService service;

    @Operation(summary = "통합 검색 실행", description = "키워드를 통해 장소를 검색하며, 해당 키워드를 사용자의 최근 검색어에 저장합니다.")
    @GetMapping
    public ResponseEntity<?> search(
            @Parameter(description = "검색 키워드", example = "애견 동반 식당") @RequestParam String keyword,
            @Parameter(description = "사용자 ID") @RequestParam Long userId
    ) {
        return ResponseEntity.ok(service.search(keyword, userId));
    }

    @Operation(summary = "최근 검색어 조회", description = "특정 사용자의 최근 검색어 목록을 가져옵니다.")
    @GetMapping("/recent")
    public ResponseEntity<?> recent(
            @Parameter(description = "사용자 ID") @RequestParam Long userId
    ) {
        return ResponseEntity.ok(service.getRecentSearches(userId));
    }

    @Operation(summary = "최근 검색어 개별 삭제", description = "최근 검색어 목록에서 특정 키워드를 삭제합니다.")
    @DeleteMapping("/recent/{keyword}")
    public ResponseEntity<?> deleteRecent(
            @Parameter(description = "삭제할 키워드") @PathVariable String keyword,
            @Parameter(description = "사용자 ID") @RequestParam Long userId
    ) {
        service.deleteRecent(userId, keyword);
        return ResponseEntity.ok(Map.of("message","삭제됨"));
    }

    @Operation(summary = "추천 검색어 조회", description = "현재 인기 있는 검색어나 시스템 추천 키워드 목록을 가져옵니다.")
    @GetMapping("/recommend")
    public ResponseEntity<?> recommend() {
        return ResponseEntity.ok(service.getRecommendKeywords());
    }
}