package com.petplace.service;

import com.petplace.dto.request.ReviewRequest;
import com.petplace.entity.*;
import com.petplace.repository.*;
import com.petplace.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @Transactional
    public Review write(Long restaurantId, Long userId, ReviewRequest req, MultipartFile image) {
        String imageUrl = null;

        if (image != null && !image.isEmpty()) {
            try {
                imageUrl = fileService.uploadFile(image);
            } catch (Exception e) {
                throw new BusinessException("이미지 업로드 중 오류가 발생했습니다.");
            }
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

    @Transactional
    public void delete(Long reviewId, Long userId) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new BusinessException("해당 리뷰를 찾을 수 없습니다."));

        if (!review.getUser().getId().equals(userId)) {
            throw new BusinessException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        if (review.getImageUrl() != null) {
            fileService.deleteFile(review.getImageUrl());
        }

        reviewRepo.delete(review);
    }

    @Transactional
    public void report(Long reviewId, Long ownerId, String reason) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new BusinessException("신고 대상 리뷰를 찾을 수 없습니다."));

        if (!review.getRestaurant().getOwner().getId().equals(ownerId)) {
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