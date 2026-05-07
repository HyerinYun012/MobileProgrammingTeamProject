package com.petplace.controller;
import com.petplace.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/search") @RequiredArgsConstructor
public class SearchController {
    private final SearchService service;

    @GetMapping
    public ResponseEntity<?> search(@RequestParam String keyword, @RequestParam Long userId) { return ResponseEntity.ok(service.search(keyword, userId)); }

    @GetMapping("/recent")
    public ResponseEntity<?> recent(@RequestParam Long userId) { return ResponseEntity.ok(service.getRecentSearches(userId)); }

    @DeleteMapping("/recent/{keyword}")
    public ResponseEntity<?> deleteRecent(@PathVariable String keyword, @RequestParam Long userId) { service.deleteRecent(userId, keyword); return ResponseEntity.ok(Map.of("message","삭제됨")); }

    @GetMapping("/recommend")
    public ResponseEntity<?> recommend() { return ResponseEntity.ok(service.getRecommendKeywords()); }
}
