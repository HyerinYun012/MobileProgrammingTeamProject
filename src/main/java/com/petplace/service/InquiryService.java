package com.petplace.service;

import com.petplace.dto.request.InquiryRequest;
import com.petplace.dto.response.InquiryDetailResponse;
import com.petplace.dto.response.InquiryResponse;
import com.petplace.entity.Inquiry;
import com.petplace.entity.Restaurant;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.InquiryRepository;
import com.petplace.repository.RestaurantRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepo;
    private final UserRepository userRepo;
    private final RestaurantRepository restaurantRepo;
    private final FileService fileService;

    @Transactional
    public void submitInquiry(Long userId, InquiryRequest req, List<MultipartFile> images) {

        if (req.getCategory() == Inquiry.Category.GENERAL && req.getRestaurantId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Restaurant restaurant = null;
        if (req.getRestaurantId() != null) {
            restaurant = restaurantRepo.findById(req.getRestaurantId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));
        }

        Inquiry.Category category = req.getCategory() != null ? req.getCategory() : Inquiry.Category.GENERAL;

        List<String> imageUrls = new ArrayList<>();
        if (images != null) {
            for (MultipartFile image : images) {
                String url = fileService.uploadFile(image);
                if (url != null) imageUrls.add(url);
            }
        }

        Inquiry inquiry = Inquiry.createInquiry(
                user,
                restaurant,
                category,
                req.getTitle(),
                req.getContent(),
                imageUrls
        );

        inquiryRepo.save(inquiry);
    }

    /**
     * 일반 사용자용 1:1 문의 단건 상세 조회 (Fetch Join 적용)
     */
    public InquiryDetailResponse getMyInquiryDetail(Long inquiryId, Long userId) {
        Inquiry inquiry = inquiryRepo.findByIdWithUserAndRestaurant(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        if (!inquiry.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_INQUIRY_ACCESS);
        }

        return InquiryDetailResponse.from(inquiry);
    }

    /**
     * 사장님용 일반 문의 단건 상세 조회 (Fetch Join 적용)
     */
    public InquiryDetailResponse getOwnerInquiryDetail(Long inquiryId, Long ownerId) {
        Inquiry inquiry = inquiryRepo.findByIdWithUserAndRestaurant(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        // 🛡️ 카테고리 검증: 사장님은 일반(GENERAL) 식당 문의만 접근 가능
        if (inquiry.getCategory() != Inquiry.Category.GENERAL) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_INQUIRY_ACCESS);
        }

        if (inquiry.getRestaurant() == null || !inquiry.getRestaurant().getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_INQUIRY_ACCESS);
        }

        return InquiryDetailResponse.from(inquiry);
    }

    /**
     * 사장님용 본인 가게 일반 문의(GENERAL) 페이징 조회
     */
    public Page<InquiryResponse> getOwnerInquiries(Long ownerId, Pageable pageable) {
        return inquiryRepo.findAllByCategoryAndRestaurantOwnerId(
                Inquiry.Category.GENERAL,
                ownerId,
                pageable
        ).map(InquiryResponse::from);
    }

    /**
     * 사장님의 '일반 문의(GENERAL)' 처리 로직
     * 본인 가게에 달린 일반 문의인지 2단계로 검증한 뒤 답변과 상태를 갱신합니다.
     */
    @Transactional
    public void processGeneralInquiry(Long inquiryId, Long ownerId, String reply) {
        Inquiry inquiry = inquiryRepo.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        // 1. 카테고리 검증: GENERAL(일반 문의)만 처리 가능
        if (inquiry.getCategory() != Inquiry.Category.GENERAL) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_INQUIRY_ACCESS);
        }

        // 2. 권한 검증: 문의 대상 식당이 있고, 그 식당의 주인이 요청한 ownerId와 일치하는지 확인
        if (inquiry.getRestaurant() == null || !inquiry.getRestaurant().getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_INQUIRY_ACCESS);
        }

        // 3. 답변 달고 처리 완료 (COMPLETED)
        inquiry.completeInquiry(reply);
    }

    /**
     * 사장님이 본인 가게의 일반 문의를 신고
     */
    @Transactional
    public void reportInquiryByOwner(Long inquiryId, Long ownerId) {
        Inquiry inquiry = inquiryRepo.findByIdWithUserAndRestaurant(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        if (inquiry.getCategory() != Inquiry.Category.GENERAL) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_INQUIRY_ACCESS);
        }

        if (inquiry.getRestaurant() == null || !inquiry.getRestaurant().getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_INQUIRY_ACCESS);
        }

        inquiry.reportByOwner();
    }

    public Page<InquiryResponse> getMyInquiries(Long userId, Pageable pageable) {
        // 1. 유저 존재 여부 확인
        if (!userRepo.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return inquiryRepo.findAllByUserId(userId, pageable)
                .map(InquiryResponse::from);
    }
}