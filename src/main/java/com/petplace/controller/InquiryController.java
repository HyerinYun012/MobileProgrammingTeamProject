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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * 💡 [신규 추가] 로그인한 유저 본인의 1:1 문의 리스트 조회
     * 엔드포인트: GET /api/inquiries/my
     */
    @Operation(
            summary = "내 1:1 문의 내역 조회",
            description = "마이페이지 혹은 고객센터화면에서 현재 로그인한 사용자가 과거에 제출했던 1:1 문의 리스트와 답변 처리 상태를 최신순으로 안전하게 조회합니다."
    )
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> getMyInquiries(
            // 🛡️ 파라미터 변조(IDOR 공격)를 원천 차단하기 위해 인증 세션의 유저 고유 식별자만 신뢰합니다.
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        List<InquiryResponse> responses = inquiryService.getMyInquiries(userId);
        return ResponseEntity.ok(ApiResponse.success("내 문의 내역 조회가 완료되었습니다.", responses));
    }
}