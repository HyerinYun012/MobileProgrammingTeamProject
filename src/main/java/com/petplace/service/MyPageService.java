package com.petplace.service;

import com.petplace.dto.request.UpdateProfileRequest;
import com.petplace.dto.response.MyReviewResponse;
import com.petplace.dto.response.UserProfileResponse;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.ReviewRepository;
import com.petplace.repository.UserRepository;
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
     * 2. 프로필 수정 (null 방어막 및 트랜잭션 롤백-S3 동기화 대책 적용)
     */
    @Transactional(rollbackFor = Exception.class) // 💡 명시적인 전역 예외 롤백 지정
    public void updateProfile(Long userId, UpdateProfileRequest req, MultipartFile profileImage) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String oldProfileUrl = user.getProfileUrl();         // 커밋 성공 시 삭제할 기존 이미지 URL 백업
        List<String> newlyUploadedFiles = new ArrayList<>(); // 롤백 발생 시 회수할 새 이미지 추적 리스트

        String nextProfileUrl = oldProfileUrl;

        // 💡 규칙 4: 비어있는 파일 유입 차단 방어막
        if (profileImage != null && !profileImage.isEmpty()) {
            String imageUrl = fileService.uploadFile(profileImage);

            // 🌟 [핵심 null 반환 예외 방어] S3 업로드가 완벽히 성공해서 URL이 리턴되었을 때만 주소 갱신 및 추적 등록
            if (imageUrl != null) {
                nextProfileUrl = imageUrl;
                newlyUploadedFiles.add(imageUrl); // 예외 발생 시 회수 대상에 추가
            }
        }

        // 도메인 엔티티 상태 변경 요청
        user.updateProfileInfo(req.nickname(), req.email(), req.phone(), nextProfileUrl);

        // ★ 규칙 4: 트랜잭션 롤백-S3 동기화 대책 동형 매핑
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 1. 최종 DB 커밋이 완료되었고 새 이미지가 성공적으로 장착되었다면, 구버전 S3 파일을 영구 제거 (용량 관리 및 엑스박스 예방)
                if (!newlyUploadedFiles.isEmpty() && oldProfileUrl != null && !oldProfileUrl.isEmpty()) {
                    fileService.deleteFile(oldProfileUrl);
                }
            }

            @Override
            public void afterCompletion(int status) {
                // 2. 비즈니스 로직 오류나 DB 제약조건 위반으로 트랜잭션이 롤백되었다면, 방금 올린 따끈따끈한 새 S3 파일을 자동 제거하여 청소
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
                        review.getCreatedAt()
                ));
    }
}