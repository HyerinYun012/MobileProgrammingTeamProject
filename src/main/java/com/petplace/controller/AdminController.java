package com.petplace.controller;

import com.petplace.dto.response.ApiResponse;
import com.petplace.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자(Admin) API", description = "신고 관리, 문의 답변, 사장님 승인 등 관리자 전용 기능")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "사장님 입점 승인", description = "회원가입한 사장님의 사업자 정보를 확인하고 계정을 활성화합니다.")
    @PatchMapping("/owners/{ownerId}/verify")
    public ResponseEntity<ApiResponse<Void>> verifyOwner(
            @AuthenticationPrincipal Long adminId, // [추가] 처리한 관리자 ID
            @Parameter(description = "승인할 사장님의 ID") @PathVariable Long ownerId) {

        adminService.verifyOwner(ownerId, adminId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "1:1 문의 처리 완료", description = "사용자의 문의 내역을 확인하고 처리 상태를 '처리완료'로 변경합니다.")
    @PatchMapping("/inquiries/{inquiryId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeInquiry(
            @AuthenticationPrincipal Long adminId, // [추가] 처리한 관리자 ID
            @Parameter(description = "처리할 문의 ID") @PathVariable Long inquiryId) {

        adminService.updateInquiryStatus(inquiryId, adminId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "신고된 리뷰 삭제", description = "신고가 정당할 경우 리뷰를 삭제하고 해당 신고 건들을 처리완료합니다.")
    @DeleteMapping("/reports/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal Long adminId, // [추가] 삭제한 관리자 ID
            @Parameter(description = "삭제할 리뷰의 ID") @PathVariable Long reviewId) {

        adminService.deleteReportedReview(reviewId, adminId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "신고 반려/단순 완료", description = "리뷰를 삭제하지 않고 신고 내역만 '처리완료' 상태로 변경합니다.")
    @PatchMapping("/reports/{reportId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeReport(
            @AuthenticationPrincipal Long adminId, // [추가] 처리한 관리자 ID
            @Parameter(description = "처리 완료할 신고 ID") @PathVariable Long reportId) {

        adminService.completeReport(reportId, adminId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}