package com.petplace.service;

import com.petplace.dto.request.InquiryRequest;
import com.petplace.dto.response.InquiryResponse;
import com.petplace.entity.Inquiry;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.InquiryRepository;
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

    @Transactional
    public void submitInquiry(Long userId, InquiryRequest req) {
        // 💡 ErrorCode 적용
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Inquiry.Category category = req.getCategory() != null ? req.getCategory() : Inquiry.Category.GENERAL;

        Inquiry inquiry = Inquiry.createInquiry(
                user,
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

        // 2. Pageable을 인자로 전달하고, 반환 타입을 Page<InquiryResponse>로 맞춤
        return inquiryRepo.findAllByUserId(userId, pageable)
                .map(InquiryResponse::from);
    }
}