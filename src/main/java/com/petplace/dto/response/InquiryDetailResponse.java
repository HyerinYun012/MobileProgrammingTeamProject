package com.petplace.dto.response;

import com.petplace.entity.Inquiry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "1:1 문의 상세 조회 응답 객체")
public record InquiryDetailResponse(
        @Schema(description = "문의 ID", example = "1")
        Long id,

        @Schema(description = "작성자 닉네임", example = "홍길동")
        String userName,

        @Schema(description = "문의 제목", example = "예약 취소는 어떻게 하나요?")
        String title,

        @Schema(description = "문의 카테고리 (GENERAL, BUSINESS, ERROR)", example = "GENERAL")
        String category,

        @Schema(description = "문의 상세 본문 내용")
        String content,

        @Schema(description = "대상 식당 이름 (없으면 null)", example = "멍멍이 브런치 카페")
        String restaurantName,

        @Schema(description = "첨부 이미지 URL 목록")
        List<String> imageUrls,

        @Schema(description = "사장님 또는 관리자의 답변 내용 (미답변 시 null)")
        String answer,

        @Schema(description = "처리 상태 (PENDING, COMPLETED)", example = "COMPLETED")
        String status,

        @Schema(description = "작성일")
        LocalDateTime createdAt
) {
    public static InquiryDetailResponse from(Inquiry inquiry) {
        String restaurantName = inquiry.getRestaurant() != null
                ? inquiry.getRestaurant().getName()
                : null;

        List<String> imageUrls = inquiry.getImageUrls() != null
                ? inquiry.getImageUrls()
                : List.of();

        return new InquiryDetailResponse(
                inquiry.getId(),
                inquiry.getUser() != null ? inquiry.getUser().getNickname() : "알 수 없음",
                inquiry.getTitle(),
                inquiry.getCategory() != null ? inquiry.getCategory().name() : null,
                inquiry.getContent(),
                restaurantName,
                imageUrls,
                inquiry.getReply(),
                inquiry.getStatus() != null ? inquiry.getStatus().name() : null,
                inquiry.getCreatedAt()
        );
    }
}
