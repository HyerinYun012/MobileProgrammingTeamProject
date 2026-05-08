package com.petplace.service;
import com.petplace.dto.request.ReviewRequest;
import com.petplace.entity.*;
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional
public class ReviewService {
    private final ReviewRepository reviewRepo;
    private final ReviewReportRepository reportRepo;

    public List<Review> getReviews(Long restaurantId) { return reviewRepo.findByRestaurant_IdOrderByCreatedAtDesc(restaurantId); }

    public Review write(Long restaurantId, Long userId, ReviewRequest req) {
        Review rv = new Review(); rv.setUser(new User(userId)); rv.setRestaurant(new Restaurant(restaurantId));
        rv.setRating(req.getRating()); rv.setContent(req.getContent()); rv.setImageUrl(req.getImageUrl());
        return reviewRepo.save(rv);
    }

    public void delete(Long reviewId) { reviewRepo.deleteById(reviewId); }

    public void report(Long reviewId, Long ownerId, String reason) {
        if (reportRepo.existsByReview_IdAndOwner_Id(reviewId, ownerId))
            throw new IllegalStateException("이미 신고한 리뷰");
        ReviewReport rp = new ReviewReport();
        Review rv = new Review(); rv.setId(reviewId); rp.setReview(rv);
        rp.setOwner(new User(ownerId)); rp.setReason(reason); reportRepo.save(rp);
    }
}
