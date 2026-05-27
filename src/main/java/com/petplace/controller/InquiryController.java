package com.petplace.controller;

import com.petplace.dto.request.InquiryRequest;
import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.InquiryDetailResponse;
import com.petplace.dto.response.InquiryResponse;
import com.petplace.service.InquiryService;
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

@Tag(name = "고객 문의(Inquiry) API", description = "1:1 문의하기 및 고객 지원 관련 API")
@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    /**
     * 1:1 문의 제출
     */
    @Operation(
            summary = "1:1 문의 제출",
            description = "인증된 사용자의 JWT 토큰을 기반으로 새로운 문의 사항을 접수합니다. " +
                    "카테고리(category)는 GENERAL(일반/식당 문의 - 사장님 처리), BUSINESS(제휴 문의 - 관리자 처리), ERROR(오류 신고 - 관리자 처리) 중 선택 가능하며, 미입력 시 GENERAL로 기본 설정됩니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody InquiryRequest req
    ) {
        // 인증 필터를 거친 안전한 세션 userId를 활용해 등록 작업을 위임합니다.
        inquiryService.submitInquiry(userId, req);
        return ResponseEntity.ok(ApiResponse.success("문의가 성공적으로 접수되었습니다.", null));
    }

    /**
     * 내 1:1 문의 내역 조회 (페이징 적용)
     */
    @Operation(
            summary = "내 1:1 문의 내역 조회",
            description = "로그인한 사용자가 본인이 작성한 전체 1:1 문의 리스트(카테고리 무관)를 페이징 처리하여 조회합니다."
    )
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<InquiryResponse>>> getMyInquiries(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(page = 0, size = 1, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 💡 "string" 방어 코드 유지
        if (pageable.getSort().stream().anyMatch(order -> "string".equals(order.getProperty()))) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("createdAt").descending()
            );
        }

        Page<InquiryResponse> responses = inquiryService.getMyInquiries(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("내 문의 내역 조회가 완료되었습니다.", responses));
    }

    /**
     * 💡 [신규 추가] 내 1:1 문의 단건 상세 조회
     */
    @Operation(
            summary = "내 1:1 문의 상세 조회",
            description = "로그인한 사용자가 본인이 작성한 특정 문의의 상세 내용 및 답변을 확인합니다. 타인의 문의 조회 시 권한 에러(403)가 발생합니다."
    )
    @GetMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> getMyInquiryDetail(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회할 문의 ID", example = "1") @PathVariable Long inquiryId
    ) {
        InquiryDetailResponse response = inquiryService.getMyInquiryDetail(inquiryId, userId);
        return ResponseEntity.ok(ApiResponse.success("문의 상세 내역이 성공적으로 조회되었습니다.", response));
    }
}