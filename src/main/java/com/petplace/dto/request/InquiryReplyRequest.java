package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "1:1 문의 답변 등록 요청 객체")
public class InquiryReplyRequest {

    @NotBlank(message = "답변 내용을 입력해주세요.")
    @Schema(description = "관리자 또는 사장님의 답변 내용", example = "안녕하세요, 문의하신 내용에 대해 안내해 드립니다...")
    private String reply;
}