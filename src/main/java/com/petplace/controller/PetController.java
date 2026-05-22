package com.petplace.controller;

import com.petplace.dto.request.PetRequest;
import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.PetResponse;
import com.petplace.entity.Pet;
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
@RequestMapping("/api/my/pets") // 💡 프론트엔드 코드 수정을 최소화하기 위해 기존 주소 유지
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @Operation(summary = "반려동물 목록 조회", description = "로그인한 사용자의 반려동물 리스트를 페이징하여 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PetResponse>>> getPets(
            @AuthenticationPrincipal Long userId,
            // 💡 @ParameterObject 추가 및 기본값 세팅 (page=0, size=1, createdAt,desc)
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(page = 0, size = 1, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 💡 "string" 방어 코드 추가
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

    @Operation(summary = "반려동물 추가", description = "새로운 반려동물을 등록합니다. 프로필 사진 이미지를 첨부할 수 있습니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Pet>> addPet(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestPart("request") PetRequest req,
            @Parameter(description = "반려동물 프로필 사진 (선택)")
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity.ok(ApiResponse.success(petService.addPet(userId, req, image)));
    }

    @Operation(summary = "반려동물 정보 수정", description = "반려동물의 정보를 수정합니다. 본인 소유 검증 및 S3 이미지 물리 교체가 포함됩니다.")
    @PutMapping(value = "/{petId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Pet>> updatePet(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "반려동물 ID") @PathVariable Long petId,
            @Valid @RequestPart("request") PetRequest req,
            @Parameter(description = "새로 교체할 프로필 사진 (선택)")
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity.ok(ApiResponse.success(petService.updatePet(userId, petId, req, image)));
    }
}