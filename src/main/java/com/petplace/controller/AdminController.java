package com.petplace.controller;

import com.petplace.dto.request.InquiryReplyRequest;
import com.petplace.dto.response.*;
import com.petplace.entity.CommunityReport;
import com.petplace.service.AdminService;
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

@Tag(name = "관리자(Admin) API", description = "신고 관리, 문의 답변, 사장님 승인 등 관리자 전용 기능")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 리뷰 신고 내역 목록 조회 (페이징 적용)
     */
    @Operation(summary = "리뷰 신고 내역 전체 조회", description = "관리자가 처리해야 할 모든 리뷰 신고 내역을 페이징하여 조회합니다.")
    @GetMapping("/reports/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewReportResponse>>> getAllReviewReports(
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(page = 0, size = 1, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (pageable.getSort().stream().anyMatch(order -> "string".equals(order.getProperty()))) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("createdAt").descending()
            );
        }

        Page<ReviewReportResponse> response = adminService.getAllReviewReports(pageable);
        return ResponseEntity.ok(ApiResponse.success("리뷰 신고 내역 목록이 성공적으로 조회되었습니다.", response));
    }

    @Operation(
            summary = "사장님 사업자 정보 및 매장 조회",
            description = "입점 승인을 위해 사장님의 계정 정보와 해당 사장님이 등록한 사업장(식당/카페 등)의 사업자 등록번호, 상호명 등을 함께 조회합니다."
    )
    @GetMapping("/owners/{ownerId}/business-info")
    public ResponseEntity<ApiResponse<OwnerBusinessInfoResponse>> getOwnerBusinessInfo(
            @AuthenticationPrincipal Long adminId,
            @Parameter(description = "조회할 사장님의 유저 ID", example = "1") @PathVariable Long ownerId) {

        OwnerBusinessInfoResponse response = adminService.getOwnerBusinessInfo(ownerId);
        return ResponseEntity.ok(ApiResponse.success("사장님 사업자 정보가 성공적으로 조회되었습니다.", response));
    }

    /**
     * 가입 승인 대기 중인 사장님 전체 목록 조회 API
     */
    @Operation(
            summary = "가입 승인 대기 사장님 목록 조회",
            description = "관리자 권한으로 가입 승인 대기 중인(isVerified = false) 사장님 유저 목록을 최신순으로 페이징하여 조회합니다."
    )
    @GetMapping("/owners/pending")
    public ResponseEntity<ApiResponse<Page<OwnerSummaryResponse>>> getPendingOwners(
            @AuthenticationPrincipal Long adminId,
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (pageable.getSort().stream().anyMatch(order -> "string".equals(order.getProperty()))) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("createdAt").descending()
            );
        }

        Page<OwnerSummaryResponse> response = adminService.getPendingOwners(adminId, pageable);
        return ResponseEntity.ok(ApiResponse.success("승인 대기 중인 사장님 목록이 성공적으로 조회되었습니다.", response));
    }

    /**
     * 💡 [Swagger 수정] 관리자용 1:1 문의 내역 전체 조회 (페이징 적용)
     */
    @Operation(
            summary = "관리자용 문의 내역 전체 조회",
            description = "관리자가 처리해야 할 비즈니스(BUSINESS) 및 시스템 오류(ERROR) 문의 내역을 페이징하여 조회합니다. 일반(GENERAL) 문의는 사장님 전용 API를 이용하세요."
    )
    @GetMapping("/inquiries")
    public ResponseEntity<ApiResponse<Page<InquiryResponse>>> getAllInquiries(
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(page = 0, size = 1, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (pageable.getSort().stream().anyMatch(order -> "string".equals(order.getProperty()))) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("createdAt").descending()
            );
        }

        Page<InquiryResponse> response = adminService.getAllInquiries(pageable);
        return ResponseEntity.ok(ApiResponse.success("관리자 전용 문의 내역 목록이 성공적으로 조회되었습니다.", response));
    }

    /**
     * 관리자용 문의 단건 상세 조회
     */
    @Operation(
            summary = "관리자용 문의 상세 조회",
            description = "관리자가 제휴(BUSINESS) 및 시스템 오류(ERROR) 문의의 상세 내용을 확인합니다."
    )
    @GetMapping("/inquiries/{inquiryId}")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> getAdminInquiryDetail(
            @AuthenticationPrincipal Long adminId,
            @Parameter(description = "조회할 문의 ID", example = "1") @PathVariable Long inquiryId
    ) {
        InquiryDetailResponse response = adminService.getInquiryDetail(inquiryId, adminId);
        return ResponseEntity.ok(ApiResponse.success("문의 상세 내역이 성공적으로 조회되었습니다.", response));
    }

    /**
     * 커뮤니티 신고 내역 조건별 목록 조회 (페이징 적용)
     */
    @Operation(summary = "커뮤니티 신고 내역 목록 조회", description = "접수된 커뮤니티 신고 내역을 상태별로 페이징 조회합니다. status 미입력 시 PENDING(처리 대기) 목록을 반환합니다.")
    @GetMapping("/reports/community")
    public ResponseEntity<ApiResponse<Page<CommunityReportResponse>>> getCommunityReports(
            @Parameter(description = "신고 처리 상태 (PENDING, COMPLETED) — 미입력 시 PENDING")
            @RequestParam(required = false, defaultValue = "PENDING") CommunityReport.Status status,
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(page = 0, size = 100, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (pageable.getSort().stream().anyMatch(order -> "string".equals(order.getProperty()))) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("createdAt").descending()
            );
        }

        Page<CommunityReportResponse> response = adminService.getCommunityReportsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("커뮤니티 신고 내역 목록이 성공적으로 조회되었습니다.", response));
    }

    @Operation(summary = "사장님 입점 승인", description = "회원가입한 사장님의 사업자 정보를 확인하고 계정을 활성화합니다.")
    @PatchMapping("/owners/{ownerId}/verify")
    public ResponseEntity<ApiResponse<Void>> verifyOwner(
            @AuthenticationPrincipal Long adminId,
            @Parameter(description = "승인할 사장님의 ID") @PathVariable Long ownerId) {

        adminService.verifyOwner(ownerId, adminId);
        return ResponseEntity.ok(ApiResponse.success("사장님 입점 승인이 완료되었습니다.", null));
    }

    /**
     * 💡 [Swagger 및 파라미터 수정] 1:1 문의 처리 완료 (답변 추가)
     */
    @Operation(
            summary = "관리자용 문의 처리 완료",
            description = "비즈니스(BUSINESS) 및 시스템 오류(ERROR) 문의에 답변을 달고 상태를 '처리완료'로 변경합니다. 사장님 담당인 일반(GENERAL) 문의 요청 시 권한 에러(403)가 발생합니다."
    )
    @PatchMapping("/inquiries/{inquiryId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeInquiry(
            @AuthenticationPrincipal Long adminId,
            @Parameter(description = "처리할 문의 ID") @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryReplyRequest req) {

        // 💡 RequestBody에서 reply를 꺼내어 Service로 전달
        adminService.updateInquiryStatus(inquiryId, adminId, req.getReply());
        return ResponseEntity.ok(ApiResponse.success("문의 처리가 성공적으로 완료되었습니다.", null));
    }

    @Operation(summary = "신고된 리뷰 삭제", description = "신고가 정당할 경우 리뷰를 삭제하고 해당 신고 건들을 처리완료합니다.")
    @DeleteMapping("/reports/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal Long adminId,
            @Parameter(description = "삭제할 리뷰의 ID") @PathVariable Long reviewId) {

        adminService.deleteReportedReview(reviewId, adminId);
        return ResponseEntity.ok(ApiResponse.success("신고된 리뷰가 강제 삭제되었습니다.", null));
    }

    @Operation(summary = "리뷰 신고 반려/단순 완료", description = "리뷰를 삭제하지 않고 신고 내역만 '처리완료' 상태로 변경합니다.")
    @PatchMapping("/reports/{reportId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeReport(
            @AuthenticationPrincipal Long adminId,
            @Parameter(description = "처리 완료할 신고 ID") @PathVariable Long reportId) {

        adminService.completeReport(reportId, adminId);
        return ResponseEntity.ok(ApiResponse.success("리뷰 신고 내역이 정상 종결되었습니다.", null));
    }

    @Operation(summary = "신고된 커뮤니티 게시글 강제 삭제", description = "관리자 권한으로 부적절한 게시글을 강제 삭제하고 관련 신고들을 종결합니다.")
    @DeleteMapping("/community/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deleteReportedPost(
            @AuthenticationPrincipal Long adminId,
            @Parameter(description = "강제 삭제할 커뮤니티 게시글 ID", example = "10") @PathVariable Long postId) {

        adminService.deleteReportedPost(postId, adminId);
        return ResponseEntity.ok(ApiResponse.success("신고된 게시글이 성공적으로 강제 삭제되었습니다.", null));
    }

    @Operation(summary = "신고된 커뮤니티 댓글 강제 삭제", description = "관리자 권한으로 부적절한 댓글을 강제 삭제하고 관련 신고들을 종결합니다.")
    @DeleteMapping("/community/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteReportedComment(
            @AuthenticationPrincipal Long adminId,
            @Parameter(description = "강제 삭제할 커뮤니티 댓글 ID", example = "25") @PathVariable Long commentId) {

        adminService.deleteReportedComment(commentId, adminId);
        return ResponseEntity.ok(ApiResponse.success("신고된 댓글이 성공적으로 강제 삭제되었습니다.", null));
    }

    @Operation(summary = "커뮤니티 신고 반려/단순 완료", description = "신고 내역만 '처리완료' 상태로 변경합니다.")
    @PatchMapping("/community/reports/{reportId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeCommunityReport(
            @AuthenticationPrincipal Long adminId,
            @Parameter(description = "처리 완료할 커뮤니티 신고 ID", example = "5") @PathVariable Long reportId) {

        adminService.completeCommunityReport(reportId, adminId);
        return ResponseEntity.ok(ApiResponse.success("커뮤니티 신고 내역이 정상적으로 종결되었습니다.", null));
    }

    @DeleteMapping("/owners/{ownerId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectOwner(
            @PathVariable Long ownerId,
            @AuthenticationPrincipal Long adminId) {

        adminService.rejectOwner(ownerId, adminId);
        return ResponseEntity.ok(ApiResponse.success("사장님 승인이 반려(계정 삭제)되었습니다.", null));
    }
}