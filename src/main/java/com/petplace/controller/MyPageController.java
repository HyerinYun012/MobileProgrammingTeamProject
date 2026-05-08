package com.petplace.controller;
import com.petplace.dto.request.*;
import com.petplace.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/my") @RequiredArgsConstructor
public class MyPageController {
    private final MyPageService service;

    @GetMapping("/profile")
    public ResponseEntity<?> profile(@RequestParam Long userId) { return ResponseEntity.ok(service.getProfile(userId)); }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestParam Long userId, @RequestBody UpdateProfileRequest req) { service.updateProfile(userId, req); return ResponseEntity.ok(Map.of("message","저장 완료")); }

    @GetMapping("/bookmarks")
    public ResponseEntity<?> bookmarks(@RequestParam Long userId) { return ResponseEntity.ok(service.getBookmarks(userId)); }

    @PostMapping("/bookmarks/{restaurantId}")
    public ResponseEntity<?> toggleBookmark(@PathVariable Long restaurantId, @RequestParam Long userId) { return ResponseEntity.ok(service.toggleBookmark(userId, restaurantId)); }

    @GetMapping("/recent")
    public ResponseEntity<?> recent(@RequestParam Long userId) { return ResponseEntity.ok(service.getRecentViews(userId)); }

    @PostMapping("/recent/{restaurantId}")
    public ResponseEntity<?> addRecent(@PathVariable Long restaurantId, @RequestParam Long userId) { service.addRecentView(userId, restaurantId); return ResponseEntity.ok(Map.of("message","기록됨")); }

    @GetMapping("/reviews")
    public ResponseEntity<?> myReviews(@RequestParam Long userId) { return ResponseEntity.ok(service.getMyReviews(userId)); }

    @GetMapping("/pets")
    public ResponseEntity<?> pets(@RequestParam Long userId) { return ResponseEntity.ok(service.getPets(userId)); }

    @PostMapping("/pets")
    public ResponseEntity<?> addPet(@RequestParam Long userId, @RequestBody PetRequest req) { return ResponseEntity.ok(service.addPet(userId, req)); }

    @PutMapping("/pets/{petId}")
    public ResponseEntity<?> updatePet(@PathVariable Long petId, @RequestBody PetRequest req) { return ResponseEntity.ok(service.updatePet(petId, req)); }
}
