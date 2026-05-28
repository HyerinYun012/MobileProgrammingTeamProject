package com.petplace.service;

import com.petplace.dto.request.ReviewRequest;
import com.petplace.dto.response.ReviewResponse;
import com.petplace.entity.*;
import com.petplace.repository.*;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviewRepo;
    private final ReviewReportRepository reportRepo;
    private final FileService fileService;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    public Page<ReviewResponse> getReviews(Long restaurantId, Pageable pageable) {
        return reviewRepo.findByRestaurantId(restaurantId, pageable)
                .map(ReviewResponse::from);
    }

    /**
     * 리뷰 작성
     * ★ 규칙 4 적용: null 방어 가드 및 롤백 S3 청소 공정
     */
    @Transactional(rollbackFor = Exception.class)
    public Review write(Long restaurantId, Long userId, ReviewRequest req, MultipartFile image) {
        String imageUrl = null;
        final List<String> uploadedFiles = new ArrayList<>(); // 롤백 대비 추적 리스트

        if (image != null && !image.isEmpty()) {
            String tempUrl = fileService.uploadFile(image);

            // 🌟 [핵심 null 방어]: 업로드가 성공한 경우에만 진행
            if (tempUrl != null) {
                imageUrl = tempUrl;
                uploadedFiles.add(tempUrl);
            }
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    uploadedFiles.forEach(fileService::deleteFile);
                }
            }
        });

        Review rv = Review.builder()
                .user(userRepository.getReferenceById(userId))
                .restaurant(restaurantRepository.getReferenceById(restaurantId))
                .rating(req.rating())
                .content(req.content())
                .imageUrl(imageUrl)
                .build();

        return reviewRepo.save(rv);
    }

    /**
     * 리뷰 수정
     * ★ 규칙 4 적용: 커밋/롤백 양방향 동기화 완벽 보장
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long reviewId, Long userId, ReviewRequest req, MultipartFile newImage) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!Objects.equals(review.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        final String oldImageUrl = review.getImageUrl();
        String updatedImageUrl = oldImageUrl;

        if (newImage != null && !newImage.isEmpty()) {
            String tempUrl = fileService.uploadFile(newImage);

            // 🌟 [핵심 null 방어]: 새 이미지가 성공적으로 업로드되었을 때만 처리
            if (tempUrl != null) {
                updatedImageUrl = tempUrl;
                final String newlyUploadedUrl = tempUrl; // 내부 클래스 전달용 final 변수

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            if (oldImageUrl != null) fileService.deleteFile(oldImageUrl);
                        } else if (status == STATUS_ROLLED_BACK) {
                            fileService.deleteFile(newlyUploadedUrl);
                        }
                    }
                });
            }
        }

        review.updateReview(req.rating(), req.content(), updatedImageUrl);
    }

    /**
     * 리뷰 삭제
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long reviewId, Long userId) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!Objects.equals(review.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        if (review.getImageUrl() != null) {
            final String targetUrl = review.getImageUrl();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileService.deleteFile(targetUrl);
                }
            });
        }

        reviewRepo.delete(review);
    }

    /**
     * 리뷰 신고
     */
    @Transactional(rollbackFor = Exception.class)
    public void report(Long reviewId, Long ownerId, String reason) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!Objects.equals(review.getRestaurant().getOwner().getId(), ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        if (reportRepo.existsByReviewIdAndOwnerId(reviewId, ownerId)) {
            throw new BusinessException(ErrorCode.ALREADY_REPORTED);
        }

        ReviewReport report = ReviewReport.builder()
                .review(review)
                .owner(userRepository.getReferenceById(ownerId))
                .reason(reason)
                .status(ReviewReport.Status.PENDING)
                .build();

        reportRepo.save(report);
    }
}