package com.petplace.controller;

import com.petplace.dto.request.InquiryReplyRequest;
import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.InquiryDetailResponse;
import com.petplace.dto.response.InquiryResponse;
import com.petplace.service.InquiryService; // 💡 의존성 변경
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    // 💡 OwnerInquiryService -> InquiryService로 일원화
    private final InquiryService inquiryService;

    @Operation(summary = "일반 문의 내역 전체 조회", description = "사장님이 처리해야 할 모든 일반(GENERAL) 문의 내역을 페이징하여 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InquiryResponse>>> getOwnerInquiries(
            @Parameter(hidden = true) @AuthenticationPrincipal Long ownerId,
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (pageable.getSort().stream().anyMatch(order -> "string".equals(order.getProperty()))) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("createdAt").descending()
            );
        }
        // 💡 변경된 서비스 메서드 호출
        Page<InquiryResponse> response = inquiryService.getOwnerInquiries(ownerId, pageable);
        return ResponseEntity.ok(ApiResponse.success("일반 문의 내역 목록이 성공적으로 조회되었습니다.", response));
    }

    /**
     * 💡 [신규 추가] 사장님용 일반 문의 단건 상세 조회
     */
    @Operation(
            summary = "일반 문의 상세 조회",
            description = "사장님이 본인 식당에 접수된 특정 일반(GENERAL) 문의의 상세 내용을 확인합니다. 타인 식당의 문의인 경우 권한 에러(403)가 발생합니다."
    )
    @GetMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> getOwnerInquiryDetail(
            @Parameter(hidden = true) @AuthenticationPrincipal Long ownerId,
            @Parameter(description = "조회할 문의 ID", example = "1") @PathVariable Long inquiryId
    ) {
        InquiryDetailResponse response = inquiryService.getOwnerInquiryDetail(inquiryId, ownerId);
        return ResponseEntity.ok(ApiResponse.success("일반 문의 상세 내역이 성공적으로 조회되었습니다.", response));
    }

    @Operation(summary = "일반 문의 처리 완료", description = "사용자의 일반 문의 사항에 답변을 달고 처리 상태를 '처리완료'로 변경합니다.")
    @PatchMapping("/{inquiryId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeInquiry(
            @Parameter(hidden = true) @AuthenticationPrincipal Long ownerId,
            @Parameter(description = "처리할 문의 ID", example = "1") @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryReplyRequest req) { // 💡 답변 데이터 바인딩 추가

        // 💡 변경된 서비스 메서드 호출 및 reply 전달
        inquiryService.processGeneralInquiry(inquiryId, ownerId, req.getReply());
        return ResponseEntity.ok(ApiResponse.success("일반 문의 처리가 성공적으로 완료되었습니다.", null));
    }
}