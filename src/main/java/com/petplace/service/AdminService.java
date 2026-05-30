package com.petplace.service;

import com.petplace.dto.response.*;
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
    private final RestaurantRepository restaurantRepository;
    private final RecentSearchRepository recentSearchRepository;
    private final NoticeRepository noticeRepository;
    private final BookmarkRepository bookmarkRepository;

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
     * 사장님 승인 거절 (계정 삭제 처리)
     */
    public void rejectOwner(Long ownerId, Long adminId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OWNER_NOT_FOUND));

        // 사장님 소유 식당 목록 조회
        java.util.List<com.petplace.entity.Restaurant> restaurants =
                restaurantRepository.findAllByOwnerId(ownerId);

        for (com.petplace.entity.Restaurant restaurant : restaurants) {
            Long restaurantId = restaurant.getId();

            // 1. 리뷰 신고 삭제 (Review cascade 전에 먼저 처리)
            reportRepository.deleteAllByRestaurantId(restaurantId);

            // 2. 공지사항 삭제 (cascade 없음)
            noticeRepository.deleteAllByRestaurantId(restaurantId);

            // 3. 북마크 삭제 (cascade 없음)
            bookmarkRepository.deleteAllByRestaurantId(restaurantId);

            // 4. 문의의 식당 참조 null 처리 (문의 내역 자체는 보존)
            inquiryRepository.clearRestaurantReference(restaurantId);
        }

        // 5. 식당 삭제 (images, reviews, menus, operatingHours 자동 cascade)
        restaurantRepository.deleteAll(restaurants);

        // 6. 최근 검색어 삭제
        recentSearchRepository.deleteByUserId(ownerId);

        // 7. 사용자 삭제
        userRepository.delete(owner);

        log.warn("Admin {} rejected and deleted owner account: {}", adminId, ownerId);
    }

    /**
     * 가입 승인 대기 중인 사장님 목록 조회 (페이징 적용)
     */
    @Transactional(readOnly = true)
    public Page<OwnerSummaryResponse> getPendingOwners(Long adminId, Pageable pageable) {
        log.info("Admin {} viewed pending owners list", adminId);

        return userRepository.findAllByRoleAndIsVerifiedFalse(User.Role.OWNER, pageable)
                .map(OwnerSummaryResponse::from);
    }

    /**
     * 사장님 입점 승인을 위한 사업자 정보 조회
     */
    @Transactional(readOnly = true)
    public OwnerBusinessInfoResponse getOwnerBusinessInfo(Long ownerId) {
        // 1. 사장님(User) 정보 조회
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OWNER_NOT_FOUND));

        // 2. 사장님이 등록한 사업장(Restaurant) 목록 조회
        List<Restaurant> restaurants = restaurantRepository.findAllByOwnerId(ownerId);

        // 3. 응답 DTO 조합
        return OwnerBusinessInfoResponse.of(owner, restaurants);
    }

    /**
     * 💡 관리자용 문의 상태 업데이트 (비즈니스/오류 문의만 처리 가능 + 답변 추가)
     */
    public void updateInquiryStatus(Long inquiryId, Long adminId, String reply) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        // 💡 일반 문의(GENERAL)는 관리자가 처리 불가 (가게 사장님만 가능)
        if (inquiry.getCategory() == Inquiry.Category.GENERAL) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_INQUIRY_ACCESS);
        }

        // 답변 등록 및 상태 완료 처리
        inquiry.completeInquiry(reply);
        log.info("Admin {} completed inquiry {} with reply", adminId, inquiryId);
    }

    /**
     * 관리자용 전체 문의 내역 페이징 조회 (BUSINESS, ERROR만 조회)
     */
    @Transactional(readOnly = true)
    public Page<InquiryResponse> getAllInquiries(Pageable pageable) {
        List<Inquiry.Category> adminCategories = List.of(Inquiry.Category.BUSINESS, Inquiry.Category.ERROR);

        return inquiryRepository.findAllByCategoryIn(adminCategories, pageable)
                .map(InquiryResponse::from);
    }

    /**
     * 관리자용 문의 단건 상세 조회 (Fetch Join 적용)
     */
    @Transactional(readOnly = true)
    public InquiryDetailResponse getInquiryDetail(Long inquiryId, Long adminId) {

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (admin.getRole() != User.Role.ADMIN) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_INQUIRY_ACCESS);
        }

        Inquiry inquiry = inquiryRepository.findByIdWithUserAndRestaurant(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        return InquiryDetailResponse.from(inquiry);
    }

    /**
     * 관리자용 리뷰 신고 내역 전체 페이징 조회
     */
    @Transactional(readOnly = true)
    public Page<ReviewReportResponse> getAllReviewReports(Pageable pageable) {
        return reportRepository.findAllBy(pageable)
                .map(report -> {
                    Review review = report.getReview();

                    return new ReviewReportResponse(
                            report.getId(),
                            review != null ? review.getId() : null,
                            review != null ? review.getContent() : "삭제된 리뷰입니다.",
                            report.getOwner() != null ? report.getOwner().getName() : "탈퇴한 사장님",
                            report.getReason(),
                            report.getStatus() != null ? report.getStatus().name() : "PENDING",
                            report.getCreatedAt(),

                            // 🌟 신규 필드 데이터 추가 바인딩 (null 방어 완료)
                            review != null ? review.getRating() : null,
                            (review != null && review.getRestaurant() != null) ? review.getRestaurant().getId() : null
                    );
                });
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
        // 1. 리뷰 존재 확인
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // 2. 물리 파일 제거
        if (review.getImageUrl() != null) {
            fileService.deleteFile(review.getImageUrl());
        }

        // 3. 신고 내역 삭제 (FK 제약 조건 해결)
        reportRepository.deleteByReviewId(reviewId);

        // 4. 리뷰 삭제
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
     * 관리자 권한으로 신고된 커뮤니티 게시글 강제 삭제
     */
    public void deleteReportedPost(Long postId, Long adminId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 💡 수정된 부분: 다중 이미지(List)를 순회하며 삭제하도록 변경
        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
            for (String imageUrl : post.getImageUrls()) {
                fileService.deleteFile(imageUrl);
            }
        }

        // 1. 신고 내역 먼저 삭제 (이게 있어야 postRepository.delete가 성공합니다)
        communityReportRepository.deleteByPostId(postId);

        // 2. 게시글 삭제
        postRepository.delete(post);
        log.warn("Admin {} forcefully deleted reported post {}", adminId, postId);
    }

    /**
     * 관리자 권한으로 신고된 커뮤니티 댓글 강제 삭제
     */
    public void deleteReportedComment(Long commentId, Long adminId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        // 1. 신고 내역 먼저 삭제
        communityReportRepository.deleteByCommentId(commentId);

        // 2. 댓글 삭제
        commentRepository.delete(comment);
        log.warn("Admin {} forcefully deleted reported comment {}", adminId, commentId);
    }

    /**
     * 커뮤니티 게시글/댓글 신고 반려 처리
     */
    public void completeCommunityReport(Long reportId, Long adminId) {
        CommunityReport report = communityReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_REPORT_NOT_FOUND));

        report.completeReport();
        log.info("Admin {} processed community report {}", adminId, reportId);
    }
}