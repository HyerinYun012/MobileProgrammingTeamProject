package com.petplace.dto.request;

import com.petplace.entity.Inquiry;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "1:1 문의 작성 요청 객체")
public class InquiryRequest {

    @NotNull(message = "문의 카테고리를 선택해주세요.")
    @Schema(description = "문의 카테고리 (GENERAL: 식당/일반 문의, BUSINESS: 제휴 문의, ERROR: 오류 신고)",
            example = "GENERAL",
            allowableValues = {"GENERAL", "BUSINESS", "ERROR"})
    private Inquiry.Category category;

    @Schema(description = "대상 식당 ID (일반 식당 문의(GENERAL) 카테고리일 경우 필수 입력)", example = "5")
    private Long restaurantId;

    @NotBlank(message = "답변 받을 이메일 주소를 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Schema(description = "답변 받을 이메일 주소", example = "user@example.com")
    private String email;

    @NotBlank(message = "문의 내용을 입력해주세요.")
    @Schema(description = "문의 상세 내용", example = "로그인이 계속 실패합니다. 확인 부탁드려요.")
    private String content;

    @Schema(description = "첨부 이미지 URL (선택 사항)", example = "https://petplace-bucket.s3.amazon.com/inquiry/error.jpg")
    private String imageUrl;
}