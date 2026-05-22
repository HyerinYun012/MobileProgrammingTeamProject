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
    // 💡 프록시 객체 생성을 위해 Repository 의존성 추가
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    public Page<ReviewResponse> getReviews(Long restaurantId, Pageable pageable) {
        // 💡 Repository의 페이징 메서드 호출 후 DTO로 변환
        return reviewRepo.findByRestaurantId(restaurantId, pageable)
                .map(ReviewResponse::from);
    }

    /**
     * 리뷰 작성
     * 🚀 [개선] 롤백 시 S3 업로드 파일 삭제 로직 추가
     */
    @Transactional
    public Review write(Long restaurantId, Long userId, ReviewRequest req, MultipartFile image) {
        String imageUrl = null;
        List<String> uploadedFiles = new ArrayList<>(); // 롤백 대비 추적 리스트

        if (image != null && !image.isEmpty()) {
            imageUrl = fileService.uploadFile(image);
            uploadedFiles.add(imageUrl);
        }

        // 트랜잭션 롤백 시 파일 삭제 동기화
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
                .rating(req.getRating())
                .content(req.getContent())
                .imageUrl(imageUrl)
                .build();

        return reviewRepo.save(rv);
    }

    /**
     * 리뷰 수정
     * 🚀 [개선] 트랜잭션 커밋/롤백 상태에 따른 S3 파일 정합성 보장 로직 도입
     */
    @Transactional
    public void update(Long reviewId, Long userId, ReviewRequest req, MultipartFile newImage) {
        // 1. 리뷰 조회 및 권한 검증
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!Objects.equals(review.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        String oldImageUrl = review.getImageUrl();
        String updatedImageUrl = oldImageUrl;

        // 2. 새 이미지가 제공된 경우 처리
        if (newImage != null && !newImage.isEmpty()) {
            // 새 이미지 선 업로드
            updatedImageUrl = fileService.uploadFile(newImage);
            String finalNewImageUrl = updatedImageUrl;

            // 트랜잭션 생명주기에 파일 처리 동기화 조율
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) {
                        // 최종 커밋 성공 시에만 구버전 파일 안전 삭제
                        if (oldImageUrl != null) {
                            fileService.deleteFile(oldImageUrl);
                        }
                    } else if (status == STATUS_ROLLED_BACK) {
                        // 트랜잭션 실패(롤백) 시 낙관적으로 업로드했던 신규 파일 격리 삭제
                        fileService.deleteFile(finalNewImageUrl);
                    }
                }
            });
        }

        // 3. 더티 체킹을 통한 엔티티 상태 변경
        review.updateReview(req.getRating(), req.getContent(), updatedImageUrl);
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void delete(Long reviewId, Long userId) {
        // 💡 ErrorCode 적용
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // 💡 ErrorCode 적용: 권한 검증
        if (!Objects.equals(review.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        if (review.getImageUrl() != null) {
            fileService.deleteFile(review.getImageUrl());
        }

        reviewRepo.delete(review);
    }

    /**
     * 리뷰 신고
     */
    @Transactional
    public void report(Long reviewId, Long ownerId, String reason) {
        // 💡 ErrorCode 적용
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // 💡 ErrorCode 적용: 사장님 권한 검증
        if (!Objects.equals(review.getRestaurant().getOwner().getId(), ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 💡 ErrorCode 적용: 중복 신고 방지
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