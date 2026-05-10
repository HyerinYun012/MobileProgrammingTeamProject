package com.petplace.service;

import com.petplace.entity.*;
import com.petplace.exception.BusinessException;
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 사장님 승인 (처리 관리자 정보 기록)
     */
    public void verifyOwner(Long ownerId, Long adminId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException("해당 사장님 정보를 찾을 수 없습니다."));

        owner.setVerified(true);
        log.info("Admin {} verified owner {}", adminId, ownerId);
    }

    /**
     * 문의 상태 업데이트 (처리 관리자 정보 기록 가능)
     */
    public void updateInquiryStatus(Long inquiryId, Long adminId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException("해당 문의 내역이 존재하지 않습니다."));

        inquiry.setStatus(Inquiry.Status.COMPLETED); // 영어 Enum 적용
        log.info("Admin {} completed inquiry {}", adminId, inquiryId);
    }

    /**
     * 신고된 리뷰 삭제 및 물리 파일 제거
     */
    public void deleteReportedReview(Long reviewId, Long adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException("해당 리뷰를 찾을 수 없습니다."));

        // 1. 실제 물리 파일 삭제 (S3 등)
        if (review.getImageUrl() != null) {
            fileService.deleteFile(review.getImageUrl());
        }

        // 2. 해당 리뷰와 연결된 모든 신고 내역 처리완료
        reportRepository.findAllByReviewId(reviewId).forEach(report -> {
            report.setStatus(ReviewReport.Status.COMPLETED); // 프로젝트 표준에 맞춤
        });

        // 3. 리뷰 삭제 및 로그 기록
        reviewRepository.delete(review);
        log.warn("Admin {} deleted reported review {}", adminId, reviewId);
    }

    /**
     * 신고 반려/단순 완료
     */
    public void completeReport(Long reportId, Long adminId) {
        ReviewReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("해당 신고 내역이 존재하지 않습니다."));

        report.setStatus(ReviewReport.Status.COMPLETED); // 영어 Enum 적용
        log.info("Admin {} processed report {}", adminId, reportId);
    }
}