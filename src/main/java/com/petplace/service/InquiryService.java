package com.petplace.service;

import com.petplace.dto.request.InquiryRequest;
import com.petplace.dto.response.InquiryResponse; // 💡 신규 추가된 응답 DTO 임포트
import com.petplace.entity.Inquiry;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.repository.InquiryRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 💡 기본적으로 조회 전용 트랜잭션을 적용하여 성능을 최적화합니다.
public class InquiryService {

    private final InquiryRepository inquiryRepo;
    private final UserRepository userRepo;

    /**
     * 사용자의 1:1 문의사항 등록 처리
     * CUD 작업이 일어나므로 클래스 레벨의 readOnly=true를 덮어쓰기 위해 @Transactional을 선언합니다.
     */
    @Transactional
    public void submitInquiry(Long userId, InquiryRequest req) {
        // 1. 유저 존재 확인
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 사용자입니다."));

        // DTO 단계에서 이미 Inquiry.Category 타입으로 바인딩되어 들어온다고 가정합니다.
        Inquiry.Category category = req.getCategory() != null ? req.getCategory() : Inquiry.Category.GENERAL;

        // 2. 🛡️ 엔티티의 캡슐화된 정적 팩토리 메서드(createInquiry) 호출
        Inquiry inquiry = Inquiry.createInquiry(
                user,
                category,
                req.getContent(),
                req.getEmail(),
                req.getImageUrl()
        );

        // 3. 영속화 작업 수행
        inquiryRepo.save(inquiry);
    }

    /**
     * 💡 [신규 추가] 로그인한 유저 본인의 1:1 문의 내역 리스트 조회
     * 마이페이지 등에서 과거에 본인이 제출한 문의 내역 및 답변 상태(대기/완료 등)를 최신순으로 확인합니다.
     */
    public List<InquiryResponse> getMyInquiries(Long userId) {
        return inquiryRepo.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(InquiryResponse::from) // 💡 대폭 간결해진 매핑 로직
                .toList();
    }
}