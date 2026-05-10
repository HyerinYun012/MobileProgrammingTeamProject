package com.petplace.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "관리자용 문의 내역 응답 객체")
public record InquiryResponse(
        @Schema(description = "문의 ID", example = "1") Long id,
        @Schema(description = "작성자 이름", example = "홍길동") String userName,
        @Schema(description = "문의 카테고리", example = "일반문의") String category,
        @Schema(description = "문의 내용", example = "결제 취소는 어떻게 하나요?") String content,
        @Schema(description = "처리 상태", example = "대기") String status,
        @Schema(description = "작성일") LocalDateTime createdAt
) {}