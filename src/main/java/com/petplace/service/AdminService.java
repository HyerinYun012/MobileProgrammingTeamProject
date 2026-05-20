package com.petplace.service;

import com.petplace.dto.response.CommunityReportResponse;
import com.petplace.dto.response.InquiryResponse;
import com.petplace.dto.response.ReviewReportResponse;
import com.petplace.entity.*;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final InquiryRepository inquiryRepository;
    private final ReviewReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final FileService fileService;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommunityReportRepository communityReportRepository;

    /**
     * 사장님 승인
     */
    public void verifyOwner(Long ownerId, Long adminId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OWNER_NOT_FOUND));

        owner.verify();
        log.info("Admin {} verified owner {}", adminId, ownerId);
    }

    /**
     * 문의 상태 업데이트
     */
    public void updateInquiryStatus(Long inquiryId, Long adminId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        inquiry.completeInquiry();
        log.info("Admin {} completed inquiry {}", adminId, inquiryId);
    }

    /**
     * 관리자용 전체 문의 내역 페이징 조회
     */
    @Transactional(readOnly = true)
    public Page<InquiryResponse> getAllInquiries(Pageable pageable) {
        return inquiryRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(inquiry -> new InquiryResponse(
                        inquiry.getId(),
                        inquiry.getUser() != null ? inquiry.getUser().getNickname() : "알 수 없는 사용자",
                        inquiry.getCategory() != null ? inquiry.getCategory().name() : "일반",
                        inquiry.getContent(),
                        inquiry.getStatus() != null ? inquiry.getStatus().name() : "대기",
                        inquiry.getCreatedAt()
                ));
    }

    /**
     * 관리자용 리뷰 신고 내역 전체 페이징 조회
     */
    @Transactional(readOnly = true)
    public Page<ReviewReportResponse> getAllReviewReports(Pageable pageable) {
        return reportRepository.findAllBy(pageable)
                .map(report -> new ReviewReportResponse(
                        report.getId(),
                        report.getReview() != null ? report.getReview().getId() : null,
                        report.getReview() != null ? report.getReview().getContent() : "삭제된 리뷰입니다.",
                        report.getOwner() != null ? report.getOwner().getName() : "탈퇴한 사장님",
                        report.getReason(),
                        report.getStatus() != null ? report.getStatus().name() : "PENDING",
                        report.getCreatedAt()
                ));
    }

    /**
     * 관리자용 커뮤니티 신고 내역 상태별 페이징 조회
     */
    @Transactional(readOnly = true)
    public Page<CommunityReportResponse> getCommunityReportsByStatus(CommunityReport.Status status, Pageable pageable) {
        return communityReportRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable)
                .map(report -> new CommunityReportResponse(
                        report.getId(),
                        report.getPost() != null ? report.getPost().getId() : null,
                        report.getComment() != null ? report.getComment().getId() : null,
                        report.getReporter() != null ? report.getReporter().getNickname() : "알 수 없는 사용자",
                        report.getReason(),
                        report.getStatus() != null ? report.getStatus().name() : "PENDING",
                        report.getCreatedAt()
                ));
    }

    /**
     * 신고된 리뷰 삭제 및 물리 파일 제거
     */
    public void deleteReportedReview(Long reviewId, Long adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (review.getImageUrl() != null) {
            fileService.deleteFile(review.getImageUrl());
        }

        // 삭제 로직은 전체 신고 처리가 필요하므로 unpaged() 사용
        List<ReviewReport> reports = reportRepository.findAllByReviewId(reviewId, Pageable.unpaged()).getContent();
        reports.forEach(ReviewReport::completeReport);

        reviewRepository.delete(review);
        log.warn("Admin {} deleted reported review {}", adminId, reviewId);
    }

    /**
     * 리뷰 신고 반려/단순 완료
     */
    public void completeReport(Long reportId, Long adminId) {
        ReviewReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_REPORT_NOT_FOUND));

        report.completeReport();
        log.info("Admin {} processed report {}", adminId, reportId);
    }

    /**
     * 관리자 권한으로 신고된 커뮤니티 게시글 강제 삭제 + 신고 내역 종결
     */
    public void deleteReportedPost(Long postId, Long adminId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            fileService.deleteFile(post.getImageUrl());
        }

        // 전체 신고 내역 종결을 위해 unpaged() 사용
        List<CommunityReport> reports = communityReportRepository.findAllByPostId(postId, Pageable.unpaged()).getContent();
        reports.forEach(CommunityReport::completeReport);

        postRepository.delete(post);
        log.warn("Admin {} forcefully deleted reported post {}", adminId, postId);
    }

    /**
     * 관리자 권한으로 신고된 커뮤니티 댓글 강제 삭제 + 신고 내역 종결
     */
    public void deleteReportedComment(Long commentId, Long adminId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        // 전체 신고 내역 종결을 위해 unpaged() 사용
        List<CommunityReport> reports = communityReportRepository.findAllByCommentId(commentId, Pageable.unpaged()).getContent();
        reports.forEach(CommunityReport::completeReport);

        commentRepository.delete(comment);
        log.warn("Admin {} forcefully deleted reported comment {}", adminId, commentId);
    }

    /**
     * 커뮤니티 게시글/댓글 신고 반려 처리 (단순 완료)
     */
    public void completeCommunityReport(Long reportId, Long adminId) {
        CommunityReport report = communityReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_REPORT_NOT_FOUND));

        report.completeReport();
        log.info("Admin {} processed community report {}", adminId, reportId);
    }
}