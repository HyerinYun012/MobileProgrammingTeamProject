package com.petplace.service;

import com.petplace.dto.request.InquiryRequest;
import com.petplace.entity.Inquiry;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.repository.InquiryRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {
    private final InquiryRepository inquiryRepo;
    private final UserRepository userRepo;

    public void submitInquiry(Long userId, InquiryRequest req) {
        // 1. 유저 존재 확인
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 사용자입니다."));

        // 2. 엔티티 생성 및 데이터 매핑 (엔티티 필드명과 Enum 값에 맞춤)
        Inquiry i = new Inquiry();
        i.setUser(user);

        // DTO에서 가져온 값이 String이라면 Enum.valueOf() 등으로 변환 필요
        // 여기서는 req.getCategory()가 이미 엔티티의 Category 타입이라고 가정하거나
        // 기본값(Category.일반문의)을 활용합니다.
        if (req.getCategory() != null) {
            i.setCategory(req.getCategory());
        }

        i.setEmail(req.getEmail());
        i.setContent(req.getContent());
        i.setImageUrl(req.getImageUrl());

        // 엔티티 기본값이 Status.대기 이므로 별도 설정 없어도 대기로 저장됨
        i.setStatus(Inquiry.Status.PENDING);

        inquiryRepo.save(i);
    }
}