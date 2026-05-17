package com.petplace.dto.response;

import com.petplace.entity.Inquiry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "1:1 문의 내역 응답 객체 (영문 표준 데이터 표준 규격 적용)")
public record InquiryResponse(
        @Schema(description = "문의 ID", example = "1")
        Long id,

        @Schema(description = "작성자 닉네임", example = "홍길동")
        String userName,

        // 🌟 [교정] Swagger 예시값을 엔티티 Enum 스펙(GENERAL, BUSINESS, REVIEW)에 맞춤
        @Schema(description = "문의 카테고리 (GENERAL, BUSINESS, REVIEW)", example = "GENERAL")
        String category,

        @Schema(description = "문의 내용", example = "결제 취소는 어떻게 하나요?")
        String content,

        // 🌟 [교정] Swagger 예시값을 엔티티 Enum 스펙(PENDING, COMPLETED)에 맞춤
        @Schema(description = "처리 상태 (PENDING, COMPLETED)", example = "PENDING")
        String status,

        @Schema(description = "작성일")
        LocalDateTime createdAt
) {
    /**
     * 💡 엔티티를 DTO로 안전하게 역직렬화/매핑해주는 팩토리 생성자 기법
     * 엔티티의 영문 Enum 명칭(.name())을 그대로 스트링 직렬화하여 반환합니다.
     */
    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getUser() != null ? inquiry.getUser().getNickname() : "알 수 없음",
                inquiry.getCategory() != null ? inquiry.getCategory().name() : null, // 🛡️ 엔티티 영문명(GENERAL 등) 그대로 매핑
                inquiry.getContent(),
                inquiry.getStatus() != null ? inquiry.getStatus().name() : null,   // 🛡️ 엔티티 영문명(PENDING 등) 그대로 매핑
                inquiry.getCreatedAt()
        );
    }
}