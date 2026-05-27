package com.petplace.controller;

import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.InquiryResponse;
import com.petplace.service.OwnerInquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사장님(Owner) 문의 관리 API", description = "일반 고객 문의 내역 조회 및 상태 변경 관련 사장님 전용 API")
@RestController
@RequestMapping("/api/owner/inquiries")
@RequiredArgsConstructor
public class OwnerInquiryController {

    private final OwnerInquiryService ownerInquiryService;

    @Operation(summary = "일반 문의 내역 전체 조회", description = "사장님이 처리해야 할 모든 일반(GENERAL) 문의 내역을 페이징하여 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InquiryResponse>>> getOwnerInquiries(
            @Parameter(hidden = true) @AuthenticationPrincipal Long ownerId,
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(page = 0, size = 1, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // "string" 정렬 쿼리 방어 코드
        if (pageable.getSort().stream().anyMatch(order -> "string".equals(order.getProperty()))) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("createdAt").descending()
            );
        }
        Page<InquiryResponse> response = ownerInquiryService.getOwnerInquiries(ownerId, pageable);
        return ResponseEntity.ok(ApiResponse.success("일반 문의 내역 목록이 성공적으로 조회되었습니다.", response));
    }

    @Operation(summary = "일반 문의 처리 완료", description = "사용자의 일반 문의 사항을 확인한 후 처리 상태를 '처리완료'로 변경합니다.")
    @PatchMapping("/{inquiryId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeInquiry(
            @Parameter(hidden = true) @AuthenticationPrincipal Long ownerId,
            @Parameter(description = "처리할 문의 ID", example = "1") @PathVariable Long inquiryId) {

        ownerInquiryService.completeOwnerInquiry(inquiryId, ownerId);
        return ResponseEntity.ok(ApiResponse.success("문의 처리가 완료되었습니다.", null));
    }
}