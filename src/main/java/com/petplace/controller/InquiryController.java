package com.petplace.controller;

import com.petplace.dto.request.InquiryRequest;
import com.petplace.dto.response.ApiResponse;
import com.petplace.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
            description = "인증된 사용자의 토큰을 기반으로 새로운 문의 사항을 접수합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(
            @AuthenticationPrincipal Long userId, // [변경] @RequestParam 제거 및 인증 주체 주입
            @Valid @RequestBody InquiryRequest req
    ) {
        // 이제 파라미터로 userId를 가로채는 공격이 불가능해졌습니다.
        inquiryService.submitInquiry(userId, req);
        return ResponseEntity.ok(ApiResponse.success("문의가 성공적으로 접수되었습니다.", null));
    }
}