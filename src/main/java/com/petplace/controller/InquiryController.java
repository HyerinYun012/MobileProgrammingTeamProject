package com.petplace.controller;

import com.petplace.dto.request.InquiryRequest;
import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.InquiryResponse; // 💡 신규 추가된 응답 DTO 임포트
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

    @Operation(
            summary = "1:1 문의 제출",
            description = "인증된 사용자의 JWT 토큰을 기반으로 새로운 문의 사항을 안전하게 접수합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(
            // 💡 Swagger UI 상에서 인증용 토큰 변수인 userId 파라미터 필드가 불필요하게 노출되는 것을 방지합니다.
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody InquiryRequest req
    ) {
        // 인증 필터를 거친 안전한 세션 userId를 활용해 등록 작업을 위임합니다.
        inquiryService.submitInquiry(userId, req);
        return ResponseEntity.ok(ApiResponse.success("문의가 성공적으로 접수되었습니다.", null));
    }

    @Operation(
            summary = "내 1:1 문의 내역 조회",
            description = "로그인한 사용자의 1:1 문의 리스트를 페이징 처리하여 조회합니다."
    )
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<InquiryResponse>>> getMyInquiries(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            // 💡 페이징 파라미터 추가
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 💡 서비스 호출 시 pageable을 전달합니다.
        Page<InquiryResponse> responses = inquiryService.getMyInquiries(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("내 문의 내역 조회가 완료되었습니다.", responses));
    }
}