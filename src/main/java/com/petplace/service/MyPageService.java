package com.petplace.service;

import com.petplace.dto.request.UpdateProfileRequest;
import com.petplace.dto.response.MyReviewResponse;
import com.petplace.dto.response.UserProfileResponse;
import com.petplace.entity.LocalAuth;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.ReviewRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepo;
    private final ReviewRepository reviewRepo;
    private final FileService fileService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 1. 프로필 조회 (이름, 닉네임, 로그인 아이디, 이메일, 전화번호 포함)
     */
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalAuth localAuth = user.getLocalAuth();
        String loginId = (localAuth != null) ? localAuth.getLoginId() : null;

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getNickname(),
                loginId,
                user.getEmail(),
                user.getPhone(),
                user.getProfileUrl(),
                user.getRole().name()
        );
    }

    /**
     * 2. 프로필 수정 (이름, 닉네임, 이메일, 전화번호, 비밀번호 선택 변경)
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UpdateProfileRequest req, MultipartFile profileImage) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String oldProfileUrl = user.getProfileUrl();
        List<String> newlyUploadedFiles = new ArrayList<>();

        String nextProfileUrl = oldProfileUrl;
        if (profileImage != null && !profileImage.isEmpty()) {
            String imageUrl = fileService.uploadFile(profileImage);
            if (imageUrl != null) {
                nextProfileUrl = imageUrl;
                newlyUploadedFiles.add(imageUrl);
            }
        }

        user.updateProfileInfo(req.name(), req.nickname(), req.email(), req.phone(), nextProfileUrl);

        // 비밀번호 변경 (값이 있을 때만)
        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            LocalAuth localAuth = user.getLocalAuth();
            if (localAuth != null) {
                localAuth.changePassword(passwordEncoder.encode(req.newPassword()));
            }
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (!newlyUploadedFiles.isEmpty() && oldProfileUrl != null && !oldProfileUrl.isEmpty()) {
                    fileService.deleteFile(oldProfileUrl);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    newlyUploadedFiles.forEach(fileService::deleteFile);
                }
            }
        });
    }

    /**
     * 3. 내 리뷰 목록 조회
     */
    public Page<MyReviewResponse> getMyReviews(Long userId, Pageable pageable) {
        if (!userRepo.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return reviewRepo.findAllByUserId(userId, pageable)
                .map(review -> new MyReviewResponse(
                        review.getId(),
                        review.getRestaurant().getId(),
                        review.getRestaurant().getName(),
                        review.getContent(),
                        review.getRating(),
                        review.getCreatedAt(),
                        review.getImageUrl()
                ));
    }
}
