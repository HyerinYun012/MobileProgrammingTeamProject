package com.petplace.controller;

import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.OwnerRestaurantSummaryResponse;
import com.petplace.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "사장님(Owner) 업장 API", description = "사장님 본인이 등록한 업장 목록 조회 API")
@RestController
@RequestMapping("/api/owner/restaurants")
@RequiredArgsConstructor
public class OwnerRestaurantController {

    private final RestaurantService restaurantService;

    @Operation(summary = "내 업장 목록 조회", description = "로그인한 사장님 본인이 등록한 업장 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OwnerRestaurantSummaryResponse>>> getMyRestaurants(
            @Parameter(hidden = true) @AuthenticationPrincipal Long ownerId
    ) {
        List<OwnerRestaurantSummaryResponse> result = restaurantService.getMyRestaurants(ownerId);
        return ResponseEntity.ok(ApiResponse.success("내 업장 목록을 성공적으로 조회했습니다.", result));
    }
}
