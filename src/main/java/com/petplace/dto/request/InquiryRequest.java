package com.petplace.dto.request;

import com.petplace.entity.Inquiry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "1:1 문의 작성 요청 객체")
public class InquiryRequest {

    @Schema(description = "문의 카테고리", example = "SERVICE_ERROR", allowableValues = {"SERVICE_ERROR", "SUGGESTION", "PARTNERSHIP", "ETC"})
    private Inquiry.Category category;

    @Schema(description = "답변 받을 이메일 주소", example = "user@example.com")
    private String email;

    @Schema(description = "문의 상세 내용", example = "로그인이 계속 실패합니다. 확인 부탁드려요.")
    private String content;

    @Schema(description = "첨부 이미지 URL (선택 사항)", example = "https://petplace-bucket.s3.amazon.com/inquiry/error.jpg")
    private String imageUrl;
}