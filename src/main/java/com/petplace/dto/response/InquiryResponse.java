package com.petplace.dto.response;

import com.petplace.entity.Inquiry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "1:1 문의 목록 조회 응답 객체")
public record InquiryResponse(
        @Schema(description = "문의 ID (상세 페이지 이동 시 사용)", example = "1")
        Long id,

        @Schema(description = "문의 제목", example = "예약 취소는 어떻게 하나요?")
        String title,

        @Schema(description = "처리 상태 (PENDING, COMPLETED)", example = "COMPLETED")
        String status,

        @Schema(description = "작성일")
        LocalDateTime createdAt
) {
    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getStatus() != null ? inquiry.getStatus().name() : null,
                inquiry.getCreatedAt()
        );
    }
}