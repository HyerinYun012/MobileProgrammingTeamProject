package com.petplace.controller;

import com.petplace.dto.request.*;
import com.petplace.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "장소(Restaurant/Cafe) API", description = "반려견 동반 가능 장소 검색, 필터링 및 업체 등록 관리 API")
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {
    private final RestaurantService service;

    @Operation(summary = "내 주변 장소 조회", description = "현재 위도(lat)와 경도(lng)를 기준으로 반경 내의 장소를 조회합니다.")
    @GetMapping("/nearby")
    public ResponseEntity<?> nearby(
            @Parameter(description = "위도", example = "37.5665") @RequestParam double lat,
            @Parameter(description = "경도", example = "126.9780") @RequestParam double lng,
            @Parameter(description = "검색 반경 (km단위, 기본값 3.0)") @RequestParam(defaultValue="3.0") double radius
    ) {
        return ResponseEntity.ok(service.findNearby(lat, lng, radius));
    }

    @Operation(summary = "장소 키워드 검색", description = "이름, 주소 등을 키워드로 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @Parameter(description = "검색어", example = "애견카페") @RequestParam String keyword
    ) {
        return ResponseEntity.ok(service.search(keyword));
    }

    @Operation(summary = "조건 필터링 검색", description = "대형견 가능 여부, 주차 가능 여부 등 상세 조건으로 장소를 검색합니다.")
    @GetMapping("/filter")
    public ResponseEntity<?> filter(@ModelAttribute RestaurantFilterRequest req) {
        return ResponseEntity.ok(service.filter(req));
    }

    @Operation(summary = "장소 상세 정보 조회", description = "특정 장소의 상세 정보와 리뷰, 메뉴 등을 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@Parameter(description = "장소 ID") @PathVariable Long id) {
        return ResponseEntity.ok(service.getDetail(id));
    }

    @Operation(summary = "신규 장소 등록", description = "사장님 계정이 새로운 반려견 동반 장소를 등록합니다.")
    @PostMapping
    public ResponseEntity<?> register(
            @Parameter(description = "사장님(User) ID") @RequestParam Long ownerId,
            @RequestBody RestaurantRequest req
    ) {
        return ResponseEntity.ok(service.register(ownerId, req));
    }

    @Operation(summary = "장소 정보 수정", description = "등록된 장소의 정보(영업시간, 서비스 내용 등)를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @Parameter(description = "장소 ID") @PathVariable Long id,
            @RequestBody RestaurantRequest req
    ) {
        return ResponseEntity.ok(service.update(id, req));
    }
}