package com.petplace.service;

import com.petplace.dto.response.InquiryResponse;
import com.petplace.entity.Inquiry;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OwnerInquiryService {

    private final InquiryRepository inquiryRepository;

    /**
     * 💡 [수정] 사장님용 일반 문의 내역 전체 페이징 조회
     * 오직 본인의 식당 ID와 매핑된 GENERAL 카테고리 문의만 가져오도록 ownerId 조건을 주입합니다.
     */
    @Transactional(readOnly = true)
    public Page<InquiryResponse> getOwnerInquiries(Long ownerId, Pageable pageable) {
        return inquiryRepository.findAllByCategoryAndRestaurantOwnerId(Inquiry.Category.GENERAL, ownerId, pageable)
                .map(InquiryResponse::from);
    }

    /**
     * 사장님용 일반 문의 상태 업데이트
     */
    public void completeOwnerInquiry(Long inquiryId, Long ownerId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        if (inquiry.getCategory() != Inquiry.Category.GENERAL) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_INQUIRY_ACCESS);
        }

        if (inquiry.getRestaurant() == null || !inquiry.getRestaurant().getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_INQUIRY_ACCESS);
        }

        inquiry.completeInquiry();
        log.info("Owner {} completed general inquiry {}", ownerId, inquiryId);
    }
}