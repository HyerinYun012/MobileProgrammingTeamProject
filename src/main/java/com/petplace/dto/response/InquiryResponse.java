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

        @Schema(description = "작성자 이메일", example = "user@example.com")
        String email,

        @Schema(description = "문의 카테고리 (GENERAL, BUSINESS, ERROR)", example = "GENERAL")
        String category,

        @Schema(description = "문의 내용", example = "결제 취소는 어떻게 하나요?")
        String content,

        @Schema(description = "처리 상태 (PENDING, COMPLETED)", example = "PENDING")
        String status,

        @Schema(description = "작성일")
        LocalDateTime createdAt
) {
    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getUser() != null ? inquiry.getUser().getNickname() : "알 수 없음",
                inquiry.getUser() != null ? inquiry.getUser().getEmail() : "알 수 없음",
                inquiry.getCategory() != null ? inquiry.getCategory().name() : null,
                inquiry.getContent(),
                inquiry.getStatus() != null ? inquiry.getStatus().name() : null,
                inquiry.getCreatedAt()
        );
    }
}