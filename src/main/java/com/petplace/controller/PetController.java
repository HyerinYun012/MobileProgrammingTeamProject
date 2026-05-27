package com.petplace.controller;

import com.petplace.dto.request.PetRequest;
import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.PetResponse;
import com.petplace.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "반려동물(Pet) API", description = "반려동물 등록, 수정, 조회 및 프로필 이미지 관리 API")
@RestController
@RequestMapping("/api/my/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @Operation(summary = "반려동물 목록 조회", description = "로그인한 사용자의 반려동물 리스트를 페이징하여 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PetResponse>>> getPets(
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

        Page<PetResponse> response = petService.getPets(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("반려동물 목록 조회가 완료되었습니다.", response));
    }

    @Operation(summary = "반려동물 추가", description = "새로운 반려동물을 등록합니다. 텍스트 데이터(data 파트, JSON)와 이미지 파일(image 파트)을 분리하여 전송합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> addPet(
            @AuthenticationPrincipal Long userId,
            // 💡 "request" 대신 프로젝트 표준인 "data"로 파트명 대통합!
            @Valid @RequestPart("data") PetRequest req,
            @Parameter(description = "반려동물 프로필 사진 (선택)")
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        // 💡 엔티티 노출을 막고, 다른 쓰기 API들과 마찬가지로 응답 규격을 Void(null)로 일치화!
        petService.addPet(userId, req, image);
        return ResponseEntity.ok(ApiResponse.success("반려동물이 성공적으로 등록되었습니다.", null));
    }

    @Operation(summary = "반려동물 정보 수정", description = "반려동물의 정보를 수정합니다. 텍스트 데이터(data 파트, JSON)와 이미지 파일(image 파트)을 분리하여 전송합니다.")
    @PutMapping(value = "/{petId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updatePet(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "반려동물 ID") @PathVariable Long petId,
            // 💡 여기도 동일하게 "data" 파트명으로 일치화!
            @Valid @RequestPart("data") PetRequest req,
            @Parameter(description = "새로 교체할 프로필 사진 (선택)")
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        // 💡 응답 규격을 Void(null)로 통일하여 깔끔한 결과 메시지만 반환!
        petService.updatePet(userId, petId, req, image);
        return ResponseEntity.ok(ApiResponse.success("반려동물 정보가 성공적으로 수정되었습니다.", null));
    }
}