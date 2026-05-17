package com.petplace.service;

import com.petplace.dto.request.ReviewRequest;
import com.petplace.entity.*;
import com.petplace.repository.*;
import com.petplace.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException; // 💡 IOException 임포트 추가
import java.util.List;
import java.util.Objects; // 💡 Objects 임포트 추가

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviewRepo;
    private final ReviewReportRepository reportRepo;
    private final FileService fileService;

    public List<Review> getReviews(Long restaurantId) {
        return reviewRepo.findByRestaurant_IdOrderByCreatedAtDesc(restaurantId);
    }

    /**
     * 리뷰 작성
     * 💡 [수정] throws IOException을 추가하여 예외 처리를 전역 핸들러로 위임합니다.
     */
    @Transactional
    public Review write(Long restaurantId, Long userId, ReviewRequest req, MultipartFile image) throws IOException {
        String imageUrl = null;

        // 💡 [수정] 내부에 있던 구질구질한 try-catch를 완전히 걷어내고 한 줄로 심플하게 처리합니다.
        if (image != null && !image.isEmpty()) {
            imageUrl = fileService.uploadFile(image);
        }

        // Review 엔티티에 @Builder가 있어야 에러가 안 납니다.
        Review rv = Review.builder()
                .user(new User(userId))
                .restaurant(new Restaurant(restaurantId))
                .rating(req.getRating())
                .content(req.getContent())
                .imageUrl(imageUrl)
                .build();

        return reviewRepo.save(rv);
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void delete(Long reviewId, Long userId) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new BusinessException("해당 리뷰를 찾을 수 없습니다."));

        // 💡 [수정] Objects.equals를 사용하여 혹시 모를 NPE(NullPointerException) 방지 및 인텔리제이 경고 해결
        if (!Objects.equals(review.getUser().getId(), userId)) {
            throw new BusinessException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
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
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new BusinessException("신고 대상 리뷰를 찾을 수 없습니다."));

        // 💡 [수정] 여기도 사장님 ID 비교 시 Objects.equals를 적용하여 정석대로 매핑합니다.
        if (!Objects.equals(review.getRestaurant().getOwner().getId(), ownerId)) {
            throw new BusinessException("본인 가게의 리뷰만 신고할 수 있습니다.");
        }

        if (reportRepo.existsByReviewIdAndOwnerId(reviewId, ownerId)) {
            throw new BusinessException("이미 신고한 리뷰입니다.");
        }

        // ReviewReport 엔티티의 Status를 영문(PENDING)으로 바꿨으므로 이제 에러가 나지 않습니다.
        ReviewReport report = ReviewReport.builder()
                .review(review)
                .owner(new User(ownerId))
                .reason(reason)
                .status(ReviewReport.Status.PENDING)
                .build();

        reportRepo.save(report);
    }
}