package com.petplace.service;

import com.petplace.dto.request.InquiryRequest;
import com.petplace.dto.response.InquiryResponse;
import com.petplace.entity.Inquiry;
import com.petplace.entity.Restaurant;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.InquiryRepository;
import com.petplace.repository.RestaurantRepository; // 💡 [신규 추가] 레포지토리 임포트
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepo;
    private final UserRepository userRepo;
    private final RestaurantRepository restaurantRepo;

    @Transactional
    public void submitInquiry(Long userId, InquiryRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Restaurant restaurant = null;
        if (req.getRestaurantId() != null) {
            restaurant = restaurantRepo.findById(req.getRestaurantId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));
        }

        Inquiry.Category category = req.getCategory() != null ? req.getCategory() : Inquiry.Category.GENERAL;

        Inquiry inquiry = Inquiry.createInquiry(
                user,
                restaurant,
                category,
                req.getContent(),
                req.getEmail(),
                req.getImageUrl()
        );

        inquiryRepo.save(inquiry);
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