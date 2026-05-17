package com.petplace.service;

import com.petplace.dto.request.UpdateProfileRequest;
import com.petplace.dto.response.MyReviewResponse; // 💡 추가
import com.petplace.dto.response.UserProfileResponse; // 💡 추가
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.repository.ReviewRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepo;
    private final ReviewRepository reviewRepo;

    /**
     * 1. 프로필 조회 (User 엔티티 대신 UserProfileResponse DTO로 변환하여 반환)
     */
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        // 엔티티 필드를 DTO 규격에 매핑 (필드명에 맞춰 getProfileUrl() 등으로 적절히 수정 가능)
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileUrl()
        );
    }

    /**
     * 2. 프로필 수정
     */
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        user.updateProfile(req.getNickname(), req.getProfileUrl());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
    }

    /**
     * 3. 내 리뷰 목록 조회 (Review 엔티티 목록을 MyReviewResponse DTO 목록으로 변환)
     */
    public List<MyReviewResponse> getMyReviews(Long userId) {
        return reviewRepo.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(review -> new MyReviewResponse(
                        review.getId(),
                        review.getRestaurant().getId(),
                        review.getRestaurant().getName(),
                        review.getContent(),
                        review.getRating(),
                        review.getCreatedAt()
                ))
                .toList();
    }
}