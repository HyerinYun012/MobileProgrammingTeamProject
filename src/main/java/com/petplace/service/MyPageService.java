package com.petplace.service;

import com.petplace.dto.request.UpdateProfileRequest;
import com.petplace.dto.response.MyReviewResponse;
import com.petplace.dto.response.UserProfileResponse;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.repository.ReviewRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile; // 💡 물리 파일 수용을 위해 추가

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepo;
    private final ReviewRepository reviewRepo;
    private final FileService fileService; // 💡 S3 업로드를 수행할 인프라 레이어 의존성 추가

    /**
     * 1. 프로필 조회 (User 엔티티 대신 UserProfileResponse DTO로 변환하여 반환)
     */
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileUrl()
        );
    }

    /**
     * 2. 프로필 수정
     * ⭕ [교정] 컨트롤러(MyPageController)의 인수 3개 토스 체계와 일치하도록 시그니처 맵핑 완료
     * ⭕ [교정] Checked Exception(throws IOException)을 제거하고 파일 유무에 따른 동적 S3 업로드 처리
     */
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest req, MultipartFile profileImage) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        // 💡 1. 이미지 파일이 새로 들어왔는지 검증 후 S3 업로드 분기 처리
        String nextProfileUrl = user.getProfileUrl(); // 기본값은 기존 프로필 주소 유지

        if (profileImage != null && !profileImage.isEmpty()) {
            // FileService가 런타임 예외로 변환해주므로 지저분한 try-catch나 throws 문맥이 필요 없습니다.
            nextProfileUrl = fileService.uploadFile(profileImage);

            // [선택 사항] 기존에 커스텀 프로필 이미지가 존재했다면 용량 최적화를 위해 삭제 처리 연동 가능
            // if (user.getProfileUrl() != null) { fileService.deleteFile(user.getProfileUrl()); }
        }

        // 💡 2. 파편화된 Setter 및 구버전 수정을 폐쇄하고 고도화된 일괄 비즈니스 갱신 메서드 원자적 호출
        user.updateProfileInfo(req.getNickname(), req.getEmail(), req.getPhone(), nextProfileUrl);

        // 영속성 컨텍스트 관리 하에 있으므로 트랜잭션이 끝나는 시점에 Dirty Checking(변경 감지)으로 자동 반영됩니다.
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