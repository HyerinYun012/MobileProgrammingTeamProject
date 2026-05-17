package com.petplace.service;

import com.petplace.dto.response.InquiryResponse;
import com.petplace.dto.response.ReviewReportResponse;
import com.petplace.dto.response.CommunityReportResponse;
import com.petplace.entity.*;
import com.petplace.exception.BusinessException;
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional // 기본값: 모든 메서드에 쓰기(Write) 트랜잭션 적용
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
                .orElseThrow(() -> new BusinessException("해당 사장님 정보를 찾을 수 없습니다."));

        // 🌟 [수정 완료] Setter 대신 User 도메인에 캡슐화된 승인 메서드를 호출합니다.
        owner.verify();
        log.info("Admin {} verified owner {}", adminId, ownerId);
    }

    /**
     * 문의 상태 업데이트
     */
    public void updateInquiryStatus(Long inquiryId, Long adminId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException("해당 문의 내역이 존재하지 않습니다."));

        // 🌟 [수정 완료] Inquiry 엔티티 내부에 세팅된 도메인 메서드로 변환을 권장합니다.
        inquiry.completeInquiry();
        log.info("Admin {} completed inquiry {}", adminId, inquiryId);
    }

    /**
     * 관리자용 전체 문의 내역 조회
     */
    @Transactional(readOnly = true)
    public List<InquiryResponse> getAllInquiries() {
        return inquiryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(inquiry -> new InquiryResponse(
                        inquiry.getId(),
                        inquiry.getUser() != null ? inquiry.getUser().getNickname() : "알 수 없는 사용자",
                        inquiry.getCategory() != null ? inquiry.getCategory().name() : "일반",
                        inquiry.getContent(),
                        inquiry.getStatus() != null ? inquiry.getStatus().name() : "대기",
                        inquiry.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 관리자용 리뷰 신고 내역 전체 최신순 조회
     */
    @Transactional(readOnly = true)
    public List<ReviewReportResponse> getAllReviewReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(report -> new ReviewReportResponse(
                        report.getId(),
                        report.getReview() != null ? report.getReview().getId() : null,
                        report.getReview() != null ? report.getReview().getContent() : "삭제된 리뷰입니다.",
                        report.getOwner() != null ? report.getOwner().getName() : "탈퇴한 사장님",
                        report.getReason(),
                        report.getStatus() != null ? report.getStatus().name() : "PENDING",
                        report.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 관리자용 커뮤니티 신고 내역 상태별 최신순 조회
     */
    @Transactional(readOnly = true)
    public List<CommunityReportResponse> getCommunityReportsByStatus(CommunityReport.Status status) {
        return communityReportRepository.findAllByStatusOrderByCreatedAtDesc(status).stream()
                .map(report -> new CommunityReportResponse(
                        report.getId(),
                        report.getPost() != null ? report.getPost().getId() : null,
                        report.getComment() != null ? report.getComment().getId() : null,
                        report.getReporter() != null ? report.getReporter().getNickname() : "알 수 없는 사용자",
                        report.getReason(),
                        report.getStatus() != null ? report.getStatus().name() : "PENDING",
                        report.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 신고된 리뷰 삭제 및 물리 파일 제거
     */
    public void deleteReportedReview(Long reviewId, Long adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException("해당 리뷰를 찾을 수 없습니다."));

        if (review.getImageUrl() != null) {
            fileService.deleteFile(review.getImageUrl());
        }

        // 🌟 [비즈니스 교정] 리뷰가 인쇄 레벨에서 지워지더라도, '누가 신고했는지' 이력(History) 데이터는
        // 하이버네이트 더티 체킹으로 COMPLETED 상태만 변경하고 유지하는 것이 백오피스 정석입니다. (Cascade로 묶여있다면 자동 처리도 가능)
        List<ReviewReport> reports = reportRepository.findAllByReviewId(reviewId);
        reports.forEach(ReviewReport::completeReport);

        reviewRepository.delete(review);
        log.warn("Admin {} deleted reported review {}", adminId, reviewId);
    }

    /**
     * 리뷰 신고 반려/단순 완료
     */
    public void completeReport(Long reportId, Long adminId) {
        ReviewReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("해당 신고 내역이 존재하지 않습니다."));

        report.completeReport(); // 도메인 메서드 캡슐화 처리
        log.info("Admin {} processed report {}", adminId, reportId);
    }

    /**
     * 관리자 권한으로 신고된 커뮤니티 게시글 강제 삭제 + 신고 내역 종결
     */
    public void deleteReportedPost(Long postId, Long adminId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("해당 게시글을 찾을 수 없습니다."));

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            fileService.deleteFile(post.getImageUrl());
        }

        // 해당 게시글과 엮인 관리자 신고 처리 상태를 '처리완료'로 깔끔하게 종결(더티 체킹)합니다.
        List<CommunityReport> reports = communityReportRepository.findAllByPostId(postId);
        reports.forEach(CommunityReport::completeReport); //

        // DB에서 게시글 삭제 (영속성 전이에 의해 안전하게 격리 제거)
        postRepository.delete(post);
        log.warn("Admin {} forcefully deleted reported post {}", adminId, postId);
    }

    /**
     * 관리자 권한으로 신고된 커뮤니티 댓글 강제 삭제 + 신고 내역 종결
     */
    public void deleteReportedComment(Long commentId, Long adminId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("해당 댓글을 찾을 수 없습니다."));

        // 🌟 [수정 완료] @Setter 제거 스펙 준수 및 컴파일 에러 해결
        List<CommunityReport> reports = communityReportRepository.findAllByCommentId(commentId);
        reports.forEach(CommunityReport::completeReport); //

        commentRepository.delete(comment);
        log.warn("Admin {} forcefully deleted reported comment {}", adminId, commentId);
    }

    /**
     * 커뮤니티 게시글/댓글 신고 반려 처리 (단순 완료)
     */
    public void completeCommunityReport(Long reportId, Long adminId) {
        CommunityReport report = communityReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("해당 커뮤니티 신고 내역이 존재하지 않습니다."));

        // 🌟 [경고 완전 소멸] CommunityReport 엔티티의 completeReport()를 여기서 명시적으로 찌릅니다.
        // 이로써 엔티티에 남아있던 Unused 메서드 경고가 완전히 해결됩니다!
        report.completeReport(); //
        log.info("Admin {} processed community report {}", adminId, reportId);
    }
}