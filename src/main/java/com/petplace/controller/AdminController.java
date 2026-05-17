package com.petplace.controller;

import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.InquiryResponse;
import com.petplace.dto.response.ReviewReportResponse;
import com.petplace.dto.response.CommunityReportResponse; // 💡 [신규 추가] 커뮤니티 신고 응답 DTO 임포트
import com.petplace.entity.CommunityReport; // 💡 [신규 추가] Status Enum 활용을 위한 임포트
import com.petplace.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자(Admin) API", description = "신고 관리, 문의 답변, 사장님 승인 등 관리자 전용 기능")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 리뷰 신고 내역 목록 조회
     */
    @Operation(summary = "리뷰 신고 내역 전체 조회", description = "관리자가 처리해야 할 모든 리뷰 신고 내역 리스트를 최신 접수순으로 조회합니다.")
    @GetMapping("/reports/reviews")
    public ResponseEntity<ApiResponse<List<ReviewReportResponse>>> getAllReviewReports() {
        List<ReviewReportResponse> response = adminService.getAllReviewReports();
        return ResponseEntity.ok(ApiResponse.success("리뷰 신고 내역 목록이 성공적으로 조회되었습니다.", response));
    }

    /**
     * 관리자용 1:1 문의 내역 전체 조회
     */
    @Operation(summary = "1:1 문의 내역 전체 조회", description = "관리자 대시보드에서 처리해야 할 모든 1:1 문의 내역 목록을 최신순으로 조회합니다.")
    @GetMapping("/inquiries")
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> getAllInquiries() {
        List<InquiryResponse> response = adminService.getAllInquiries();
        return ResponseEntity.ok(ApiResponse.success("1:1 문의 내역 목록이 성공적으로 조회되었습니다.", response));
    }

    /**
     * 💡 [신규 추가] 커뮤니티 신고 내역 조건별 목록 조회
     * 이 엔드포인트가 연결되면서 AdminService의 getCommunityReportsByStatus() 미사용 경고가 완전히 해결됩니다.
     */
    @Operation(summary = "커뮤니티 신고 내역 목록 조회", description = "접수된 커뮤니티(게시글/댓글) 신고 내역을 상태 조건(PENDING, COMPLETED)에 따라 최신순으로 조회합니다.")
    @GetMapping("/reports/community")
    public ResponseEntity<ApiResponse<List<CommunityReportResponse>>> getCommunityReports(
            @Parameter(description = "신고 처리 상태 (PENDING, COMPLETED)", example = "PENDING")
            @RequestParam CommunityReport.Status status) {

        List<CommunityReportResponse> response = adminService.getCommunityReportsByStatus(status);
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

    @Operation(summary = "1:1 문의 처리 완료", description = "사용자의 문의 내역을 확인하고 처리 상태를 '처리완료'로 변경합니다.")
    @PatchMapping("/inquiries/{inquiryId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeInquiry(
            @AuthenticationPrincipal Long adminId,
            @Parameter(description = "처리할 문의 ID") @PathVariable Long inquiryId) {

        adminService.updateInquiryStatus(inquiryId, adminId);
        return ResponseEntity.ok(ApiResponse.success("문의 처리가 완료되었습니다.", null));
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

    @Operation(summary = "신고된 커뮤니티 게시글 강제 삭제", description = "관리자 권한으로 부적절한 게시글을 강제 삭제(S3 파일 포함)하고 관련 신고들을 종결합니다.")
    @DeleteMapping("/community/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deleteReportedPost(
            @AuthenticationPrincipal Long adminId,
            @Parameter(description = "강제 삭제할 커뮤니티 게시글 ID", example = "10") @PathVariable Long postId) {

        adminService.deleteReportedPost(postId, adminId);
        return ResponseEntity.ok(ApiResponse.success("신고된 게시글이 성공적으로 강제 삭제되었습니다.", null));
    }

    @Operation(summary = "신고된 커뮤니티 댓글 강제 삭제", description = "관리자 권한으로 부적절한 댓글을 강제 삭제(대댓글 자동 연쇄 삭제)하고 관련 신고들을 종결합니다.")
    @DeleteMapping("/community/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteReportedComment(
            @AuthenticationPrincipal Long adminId,
            @Parameter(description = "강제 삭제할 커뮤니티 댓글 ID", example = "25") @PathVariable Long commentId) {

        adminService.deleteReportedComment(commentId, adminId);
        return ResponseEntity.ok(ApiResponse.success("신고된 댓글이 성공적으로 강제 삭제되었습니다.", null));
    }

    @Operation(summary = "커뮤니티 신고 반려/단순 완료", description = "게시글이나 댓글을 삭제하지 않고, 접수된 커뮤니티 신고 내역만 '처리완료' 상태로 변경합니다.")
    @PatchMapping("/community/reports/{reportId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeCommunityReport(
            @AuthenticationPrincipal Long adminId,
            @Parameter(description = "처리 완료할 커뮤니티 신고 ID", example = "5") @PathVariable Long reportId) {

        adminService.completeCommunityReport(reportId, adminId);
        return ResponseEntity.ok(ApiResponse.success("커뮤니티 신고 내역이 정상적으로 종결되었습니다.", null));
    }
}