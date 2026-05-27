package com.petplace.service;

import com.petplace.dto.request.UpdateProfileRequest;
import com.petplace.dto.response.MyReviewResponse;
import com.petplace.dto.response.UserProfileResponse;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode; // 💡 import 추가
import com.petplace.repository.ReviewRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepo;
    private final ReviewRepository reviewRepo;
    private final FileService fileService;

    /**
     * 1. 프로필 조회
     */
    public UserProfileResponse getProfile(Long userId) {
        // 💡 ErrorCode 적용
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileUrl(),
                user.getRole().name()
        );
    }

    /**
     * 2. 프로필 수정
     */
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest req, MultipartFile profileImage) {
        // 💡 ErrorCode 적용
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String nextProfileUrl = user.getProfileUrl();

        if (profileImage != null && !profileImage.isEmpty()) {
            nextProfileUrl = fileService.uploadFile(profileImage);
        }

        user.updateProfileInfo(req.nickname(), req.email(), req.phone(), nextProfileUrl);
    }

    /**
     * 3. 내 리뷰 목록 조회
     */
    public Page<MyReviewResponse> getMyReviews(Long userId, Pageable pageable) {
        // 1. 유저 존재 여부 확인
        if (!userRepo.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. Repository에서 Page<Review>를 받아 Page<MyReviewResponse>로 변환
        // Page의 .map()은 내부 데이터를 유지하면서 타입만 변환해주므로 매우 효율적입니다.
        return reviewRepo.findAllByUserId(userId, pageable)
                .map(review -> new MyReviewResponse(
                        review.getId(),
                        review.getRestaurant().getId(),
                        review.getRestaurant().getName(),
                        review.getContent(),
                        review.getRating(),
                        review.getCreatedAt()
                ));
    }
}